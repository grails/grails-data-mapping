/* Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.datastore.mapping.model.config;

import groovy.lang.Closure;
import groovy.lang.GroovyObject;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Entity;

import org.grails.datastore.mapping.engine.internal.MappingUtils;
import org.grails.datastore.mapping.model.ClassMapping;
import org.grails.datastore.mapping.model.IdentityMapping;
import org.grails.datastore.mapping.model.IllegalMappingException;
import org.grails.datastore.mapping.model.MappingConfigurationStrategy;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.MappingFactory;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.types.*;
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher;
import org.grails.datastore.mapping.reflect.ReflectionUtils;
import org.springframework.util.StringUtils;

import static org.grails.datastore.mapping.model.config.GormProperties.*;

/**
 * <p>This implementation of the MappingConfigurationStrategy interface
 * will interpret GORM-style syntax for defining entities and associations.
 * </p>
 *
 * <p>Example in Groovy code:</p>
 *
 * <pre>
 *  <code>
 *      class Author {
 *          String name
 *          static hasMany = [books:Book]
 *      }
 *      class Book {
 *         String title
 *         static belongsTo = [author:Author]
        }
 *  </code>
 *
 * </pre>
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class GormMappingConfigurationStrategy implements MappingConfigurationStrategy {
    private static final String IDENTITY_PROPERTY = IDENTITY;
    private static final String VERSION_PROPERTY = VERSION;
    private MappingFactory propertyFactory;
    private static final Set EXCLUDED_PROPERTIES = new HashSet(Arrays.asList("class", "metaClass"));
    private boolean canExpandMappingContext = true;

    public GormMappingConfigurationStrategy(MappingFactory propertyFactory) {
        this.propertyFactory = propertyFactory;
    }

    /**
     * Whether the strategy can add new entities to the mapping context
     */
    public void setCanExpandMappingContext(boolean canExpandMappingContext) {
        this.canExpandMappingContext = canExpandMappingContext;
    }

    /**
     * Tests whether an class is a persistent entity
     *
     * Based on the same method in Grails core within the DomainClassArtefactHandler class
     * @param clazz The java class
     *
     * @return True if it is a persistent entity
     */
    public boolean isPersistentEntity(Class clazz) {
        // its not a closure
        if (clazz == null) return false;
        if (Closure.class.isAssignableFrom(clazz)) {
            return false;
        }
        if (Enum.class.isAssignableFrom(clazz)) return false;
        if (clazz.isAnnotationPresent(Entity.class)) {
            return true;
        }
        // this is done so we don't need a statically typed reference to the Grails annotation
        for (Annotation annotation : clazz.getAnnotations()) {
            if (annotation.toString().equals("@grails.persistence.Entity()")) return true;
        }
        Class testClass = clazz;
        boolean result = false;
        while (testClass != null && !testClass.equals(GroovyObject.class) &&
                   !testClass.equals(Object.class)) {
            try {
                // make sure the identify and version field exist
                testClass.getDeclaredField(IDENTITY_PROPERTY);
                testClass.getDeclaredField(VERSION_PROPERTY);

                // passes all conditions return true
                result = true;
                break;
            } catch (SecurityException e) {
                // ignore
            } catch (NoSuchFieldException e) {
                // ignore
            }
            testClass = testClass.getSuperclass();
        }
        return result;
    }

    public List<PersistentProperty> getPersistentProperties(Class javaClass, MappingContext context) {
        return getPersistentProperties(javaClass, context, null);
    }

    public List<PersistentProperty> getPersistentProperties(Class javaClass, MappingContext context, ClassMapping classMapping) {
        PersistentEntity entity = getPersistentEntity(javaClass, context, classMapping);

        if (entity != null) {
            return getPersistentProperties(entity, context, classMapping);
        }
        return Collections.emptyList();
    }

    public List<PersistentProperty> getPersistentProperties(PersistentEntity entity, MappingContext context, ClassMapping classMapping) {
        final List<PersistentProperty> persistentProperties = new ArrayList<PersistentProperty>();
        ClassPropertyFetcher cpf = ClassPropertyFetcher.forClass(entity.getJavaClass());

        // owners are the classes that own this class
        Collection embedded = getCollectionStaticProperty(cpf, EMBEDDED);
        Collection transients = getCollectionStaticProperty(cpf, TRANSIENT);

        // hasMany associations for defining one-to-many and many-to-many
        Map hasManyMap = getAssociationMap(cpf);
        // mappedBy for defining by which property an association is mapped
        Map mappedByMap = getMapStaticProperty(cpf, MAPPED_BY);
        // hasOne for declaring a one-to-one association with the foreign key in the child
        Map hasOneMap = getAssociationMap(cpf, HAS_ONE);

        for (PropertyDescriptor descriptor : cpf.getPropertyDescriptors()) {
            if (descriptor.getPropertyType() == null || descriptor.getPropertyType() == Object.class) {
                // indexed property
                continue;
            }
            if (descriptor.getReadMethod() == null || descriptor.getWriteMethod() == null) {
                // non-persistent getter or setter
                continue;
            }
            if (descriptor.getName().equals(VERSION) && !entity.isVersioned()) {
                continue;
            }

            Field field = cpf.getDeclaredField(descriptor.getName());
            if (field != null && java.lang.reflect.Modifier.isTransient(field.getModifiers())) {
                continue;
            }
            final String propertyName = descriptor.getName();
            if (isExcludedProperty(propertyName, classMapping, transients)) continue;
            Class<?> propertyType = descriptor.getPropertyType();
            Class currentPropType = propertyType;
            // establish if the property is a one-to-many
            // if it is a Set and there are relationships defined
            // and it is defined as persistent
            if (embedded.contains(propertyName)) {
                if (isCollectionType(currentPropType)) {
                    final Association association = establishRelationshipForCollection(descriptor, entity, context, hasManyMap, mappedByMap, true);
                    if (association != null) {
                        persistentProperties.add(association);
                    }
                }
                else {
                    final ToOne association = establishDomainClassRelationship(entity, descriptor, context, hasOneMap, true);
                    if (association != null) {
                        persistentProperties.add(association);
                    }
                }
            }
            else if (isCollectionType(currentPropType)) {
                final Association association = establishRelationshipForCollection(descriptor, entity, context, hasManyMap, mappedByMap, false);
                if (association != null) {
                    configureOwningSide(association);
                    persistentProperties.add(association);
                }
            }
            // otherwise if the type is a domain class establish relationship
            else if (isPersistentEntity(currentPropType)) {
                final ToOne association = establishDomainClassRelationship(entity, descriptor, context, hasOneMap, false);
                if (association != null) {
                    configureOwningSide(association);
                    persistentProperties.add(association);
                }
            }
            else if (propertyFactory.isSimpleType(propertyType)) {
                persistentProperties.add(propertyFactory.createSimple(entity, context, descriptor));
            }
            else if (MappingFactory.isCustomType(propertyType)) {
                persistentProperties.add(propertyFactory.createCustom(entity, context, descriptor));
            }
        }
        return persistentProperties;
    }

    private List getCollectionStaticProperty(ClassPropertyFetcher cpf, String property) {
        List<Collection> colls = cpf.getStaticPropertyValuesFromInheritanceHierarchy(property, Collection.class);
        if (colls == null) {
            return Collections.emptyList();
        }
        List values = new ArrayList();
        for (Collection coll : colls) {
            values.addAll(coll);
        }
        return values;
    }

    private Map getMapStaticProperty(ClassPropertyFetcher cpf, String property) {
        List<Map> maps = cpf.getStaticPropertyValuesFromInheritanceHierarchy(property, Map.class);
        if (maps == null) {
            return Collections.emptyMap();
        }
        Map values = new HashMap();
        for (int i = maps.size(); i > 0; i--) {
            Map map = maps.get(i - 1);
            values.putAll(map);
        }
        return values;
    }

    private void configureOwningSide(Association association) {
        PersistentEntity associatedEntity = association.getAssociatedEntity();
        if(associatedEntity == null) {
            association.setOwningSide(true);
        }
        else {
            if (association.isBidirectional()) {
                if (associatedEntity.isOwningEntity(association.getOwner())) {
                    association.setOwningSide(true);
                }
            }
            else {
                if (association instanceof OneToOne) {
                    if (associatedEntity.isOwningEntity(association.getOwner()))
                        association.setOwningSide(true);
                } else if (!(association instanceof Basic)) {
                    if (associatedEntity.isOwningEntity(association.getOwner())) {
                        association.setOwningSide(true);
                    }
                    else {
                        association.setOwningSide(false);
                    }
                }
            }
        }
    }

    /**
     * Evaluates the belongsTo property to find out who owns who
     */
    private Set establishRelationshipOwners(ClassPropertyFetcher cpf) {
        Set owners = new HashSet();
        owners.addAll(cpf.getStaticPropertyValuesFromInheritanceHierarchy(BELONGS_TO, Class.class));
        owners.addAll(getCollectionStaticProperty(cpf, BELONGS_TO));
        owners.addAll(getMapStaticProperty(cpf, BELONGS_TO).values());
        return owners;
    }

    private Association establishRelationshipForCollection(PropertyDescriptor property, PersistentEntity entity, MappingContext context, Map<String, Class> hasManyMap, Map mappedByMap, boolean embedded) {
        // is it a relationship
        Class relatedClassType = hasManyMap.get(property.getName());
        // try a bit harder for embedded collections (could make this the default, rendering 'hasMany' optional
        // if generics are used)
        if (relatedClassType == null && embedded) {
            Class javaClass = entity.getJavaClass();

            Class genericClass = MappingUtils.getGenericTypeForProperty(javaClass, property.getName());

            if (genericClass != null) {
                relatedClassType = genericClass;
            }
        }

        if (relatedClassType == null) {
            return propertyFactory.createBasicCollection(entity, context, property);
        }

        if (embedded) {
            if (propertyFactory.isSimpleType(relatedClassType)) {
                return propertyFactory.createBasicCollection(entity, context, property, relatedClassType);
            }
            else if (!isPersistentEntity(relatedClassType)) {
                // no point in setting up bidirectional link here, since target isn't an entity.
                EmbeddedCollection association = propertyFactory.createEmbeddedCollection(entity, context, property);
                PersistentEntity associatedEntity = getOrCreateEmbeddedEntity(entity, context, relatedClassType);
                association.setAssociatedEntity(associatedEntity);
                return association;
            }
        }
        else if (!isPersistentEntity(relatedClassType)) {
            // otherwise set it to not persistent as you can't persist
            // relationships to non-domain classes
            return propertyFactory.createBasicCollection(entity, context, property, relatedClassType);
        }

        // set the referenced type in the property
        ClassPropertyFetcher referencedCpf = ClassPropertyFetcher.forClass(relatedClassType);
        String referencedPropertyName = null;

        // if the related type is a domain class
        // then figure out what kind of relationship it is

        // check the relationship defined in the referenced type
        // if it is also a Set/domain class etc.
        Map relatedClassRelationships = getAssociationMap(referencedCpf, HAS_MANY);
        Class<?> relatedClassPropertyType = null;

        String relatedClassPropertyName = null;
        ClassPropertyFetcher cpf = ClassPropertyFetcher.forClass(entity.getJavaClass());
        // First check whether there is an explicit relationship
        // mapping for this property (as provided by "mappedBy").
        String mappingProperty = (String)mappedByMap.get(property.getName());
        if (StringUtils.hasText(mappingProperty)) {
            // First find the specified property on the related class, if it exists.
            PropertyDescriptor pd = findProperty(getPropertiesAssignableFromType(entity.getJavaClass(), referencedCpf),
                    mappingProperty);

            // If a property of the required type does not exist, search
            // for any collection properties on the related class.
            if (pd == null) {
                pd = findProperty(referencedCpf.getPropertiesAssignableToType(Collection.class), mappingProperty);
            }

            // We've run out of options. The given "mappedBy" setting is invalid.
            if (pd == null) {
                if (entity.isExternal()) {
                    return null;
                }
                throw new IllegalMappingException("Non-existent mapping property [" + mappingProperty +
                        "] specified for property [" + property.getName() +
                        "] in class [" + entity.getJavaClass().getName() + "]");
            }

            // Tie the properties together.
            relatedClassPropertyType = pd.getPropertyType();
            referencedPropertyName = pd.getName();
        }
        else {

            if (!forceUnidirectional(property, mappedByMap)) {
                // if the related type has a relationships map it may be a many-to-many
                // figure out if there is a many-to-many relationship defined
                if (isRelationshipToMany(entity, relatedClassType, relatedClassRelationships)) {
                    Map relatedClassMappedBy = getMapStaticProperty(cpf, MAPPED_BY);
                    // retrieve the relationship property
                    for (Object o : relatedClassRelationships.keySet()) {
                        String currentKey = (String) o;
                        String mappedByProperty = (String) relatedClassMappedBy.get(currentKey);
                        if (mappedByProperty != null && !mappedByProperty.equals(property.getName())) continue;
                        Class<?> currentClass = (Class<?>)relatedClassRelationships.get(currentKey);
                        if (currentClass.isAssignableFrom(entity.getJavaClass())) {
                            relatedClassPropertyName = currentKey;
                            break;
                        }
                    }
    //            Map classRelationships = cpf.getPropertyValue(HAS_MANY, Map.class);
    //
    //            if (isRelationshipToMany(entity, relatedClassType, classRelationships)) {
    //                String relatedClassPropertyName = findManyRelatedClassPropertyName(
    //                        property.getName(), referencedCpf, classRelationships, relatedClassType);

                    // if there is one defined get the type
                    if (relatedClassPropertyName != null) {
                        relatedClassPropertyType = referencedCpf.getPropertyType(relatedClassPropertyName);
                    }
                }

                // otherwise figure out if there is a one-to-many relationship by retrieving any properties that are of the related type
                // if there is more than one property then (for the moment) ignore the relationship
                if (relatedClassPropertyType == null || Collection.class.isAssignableFrom(relatedClassPropertyType)) {
                    List<PropertyDescriptor> descriptors = getPropertiesAssignableFromType(entity.getJavaClass(), referencedCpf);

                    if (descriptors.size() == 1) {
                        final PropertyDescriptor pd = descriptors.get(0);
                        relatedClassPropertyType = pd.getPropertyType();
                        referencedPropertyName = pd.getName();
                    }
                    else if (descriptors.size() > 1) {
                        // try now to use the class name by convention
                        String classPropertyName = entity.getDecapitalizedName();
                        PropertyDescriptor pd = findProperty(descriptors, classPropertyName);
                        if (pd == null) {
                            if (entity.isExternal()) {
                                return null;
                            }
                            pd = descriptors.get(0);
                        }

                        if (pd != null) {
                            relatedClassPropertyType = pd.getPropertyType();
                            referencedPropertyName = pd.getName();
                        }
                    }
                }
            }
        }

        // if its a many-to-many figure out the owning side of the relationship
        final boolean isInverseSideEntity = isPersistentEntity(relatedClassPropertyType);
        Association association = null;
        boolean many = false;
        if (embedded) {
            association = propertyFactory.createEmbeddedCollection(entity, context, property);
        }
        else if (relatedClassPropertyType == null || isInverseSideEntity) {
            // uni or bi-directional one-to-many
            association = propertyFactory.createOneToMany(entity, context, property);
        }
        else if (Collection.class.isAssignableFrom(relatedClassPropertyType) ||
                 Map.class.isAssignableFrom(relatedClassPropertyType)) {
            // many-to-many
            association = propertyFactory.createManyToMany(entity, context, property);
            ((ManyToMany)association).setInversePropertyName(relatedClassPropertyName);
            many = true;
        }
        else {
            // uni-directional one-to-many
            association = propertyFactory.createOneToMany(entity, context, property);

        }

        PersistentEntity associatedEntity = getOrCreateAssociatedEntity(entity, context, relatedClassType);
        if (many) {
            Map classRelationships = referencedCpf.getPropertyValue(HAS_MANY, Map.class);
            referencedPropertyName = findManyRelatedClassPropertyName(null,
                    referencedCpf, classRelationships, entity.getJavaClass());
        }

        if (association != null) {
            association.setAssociatedEntity(associatedEntity);
            if (referencedPropertyName != null) {
                // bidirectional
                association.setReferencedPropertyName(referencedPropertyName);
            }
        }
        return association;
    }

    private List<PropertyDescriptor> getPropertiesAssignableFromType(Class type, ClassPropertyFetcher propertyFetcher) {
        List<PropertyDescriptor> props = propertyFetcher.getPropertiesAssignableFromType(type);
        // exclude properties of type object!
        List<PropertyDescriptor> valid = new ArrayList<PropertyDescriptor>(props.size());
        for (PropertyDescriptor prop : props) {
            if (prop.getPropertyType() != null && !prop.getPropertyType().equals(Object.class)) {
                valid.add(prop);
            }
        }
        return valid;
    }

    private String findManyRelatedClassPropertyName(String propertyName,
            ClassPropertyFetcher cpf, Map classRelationships, Class<?> classType) {
        Map mappedBy = getMapStaticProperty(cpf, MAPPED_BY);
        // retrieve the relationship property
        for (Object o : classRelationships.keySet()) {
            String currentKey = (String) o;
            String mappedByProperty = (String)mappedBy.get(currentKey);
            if (mappedByProperty != null && !mappedByProperty.equals(propertyName)) continue;
            Class<?> currentClass = (Class<?>)classRelationships.get(currentKey);
            if (currentClass.isAssignableFrom(classType)) {
                return currentKey;
            }
        }
        return null;
    }

    /**
     * Find out if the relationship is a 1-to-many or many-to-many.
     *
     * @param relatedClassType The related type
     * @param relatedClassRelationships The related types relationships
     * @return <code>true</code> if the relationship is a many-to-many
     */
    private boolean isRelationshipToMany(PersistentEntity entity,
            Class<?> relatedClassType, Map relatedClassRelationships) {
        return relatedClassRelationships != null &&
               !relatedClassRelationships.isEmpty() &&
               !relatedClassType.equals(entity.getJavaClass());
    }

    /**
     * Finds a property type is an array of descriptors for the given property name
     *
     * @param descriptors The descriptors
     * @param propertyName The property name
     * @return The Class or null
     */
    private PropertyDescriptor findProperty(List<PropertyDescriptor> descriptors, String propertyName) {
        for (PropertyDescriptor descriptor : descriptors) {
            if (descriptor.getName().equals(propertyName)) {
                return descriptor;
            }
        }
        return null;
    }

    /**
     * Establish relationship with related domain class
     *
     * @param entity
     * @param property Establishes a relationship between this class and the domain class property
     * @param context
     * @param hasOneMap
     * @param embedded
     */
    private ToOne establishDomainClassRelationship(PersistentEntity entity, PropertyDescriptor property, MappingContext context, Map hasOneMap, boolean embedded) {
        ToOne association = null;
        Class propType = property.getPropertyType();

        if (embedded && !isPersistentEntity(propType)) {
            // uni-directional to embedded non-entity
            PersistentEntity associatedEntity = getOrCreateEmbeddedEntity(entity, context, propType);
            association = propertyFactory.createEmbedded(entity, context, property);
            association.setAssociatedEntity(associatedEntity);
            return association;
        }

        ClassPropertyFetcher cpf = ClassPropertyFetcher.forClass(propType);

        // establish relationship to type
        Map relatedClassRelationships = getAllAssociationMap(cpf);
        Map mappedBy = getMapStaticProperty(cpf, MAPPED_BY);

        Class<?> relatedClassPropertyType = null;

        // if there is a relationships map use that to find out
        // whether it is mapped to a Set
        String relatedClassPropertyName = null;

        if (!forceUnidirectional(property, mappedBy)) {
            if (relatedClassRelationships != null && !relatedClassRelationships.isEmpty()) {

                PropertyDescriptor[] descriptors = ReflectionUtils.getPropertiesOfType(entity.getJavaClass(), propType);
                relatedClassPropertyName = findOneToManyThatMatchesType(entity, relatedClassRelationships);
                // if there is only one property on many-to-one side of the relationship then
                // try to establish if it is bidirectional
                if (descriptors.length == 1 && isNotMappedToDifferentProperty(property,relatedClassPropertyName, mappedBy)) {
                    if (StringUtils.hasText(relatedClassPropertyName)) {
                        // get the type of the property
                        relatedClassPropertyType = cpf.getPropertyType(relatedClassPropertyName);
                    }
                }
                // if there is more than one property on the many-to-one side then we need to either
                // find out if there is a mappedBy property or whether a convention is used to decide
                // on the mapping property
                else if (descriptors.length > 1) {
                    if (mappedBy.containsValue(property.getName())) {
                        for (Object o : mappedBy.keySet()) {
                            String mappedByPropertyName = (String) o;
                            if (property.getName().equals(mappedBy.get(mappedByPropertyName))) {
                                Class<?> mappedByRelatedType = (Class<?>) relatedClassRelationships.get(mappedByPropertyName);
                                if (mappedByRelatedType != null && propType.isAssignableFrom(mappedByRelatedType))
                                    relatedClassPropertyType = cpf.getPropertyType(mappedByPropertyName);
                            }
                        }
                    }
                    else {
                        String classNameAsProperty = Introspector.decapitalize(propType.getName());
                        if (property.getName().equals(classNameAsProperty) && !mappedBy.containsKey(relatedClassPropertyName)) {
                            relatedClassPropertyType = cpf.getPropertyType(relatedClassPropertyName);
                        }
                    }
                }
            }

            // otherwise retrieve all the properties of the type from the associated class
            if (relatedClassPropertyType == null) {
                List<PropertyDescriptor> descriptors = getPropertiesAssignableFromType(entity.getJavaClass(), cpf);

                // if there is only one then the association is established
                if (descriptors.size() == 1) {
                    relatedClassPropertyType = descriptors.get(0).getPropertyType();
                    relatedClassPropertyName = descriptors.get(0).getName();
                }
            }
        }

        //    establish relationship based on this type

        final boolean isAssociationEntity = isPersistentEntity(relatedClassPropertyType);
        // one-to-one
        if (relatedClassPropertyType == null || isAssociationEntity) {
            association = embedded ? propertyFactory.createEmbedded(entity, context, property) :
                    propertyFactory.createOneToOne(entity, context, property);

            if (hasOneMap.containsKey(property.getName()) && !embedded) {
                association.setForeignKeyInChild(true);
            }
        }
        // bi-directional many-to-one
        else if (!embedded && Collection.class.isAssignableFrom(relatedClassPropertyType)||Map.class.isAssignableFrom(relatedClassPropertyType)) {
            association = propertyFactory.createManyToOne(entity, context, property);
        }

        // bi-directional
        if (association != null) {
            PersistentEntity associatedEntity = getOrCreateAssociatedEntity(entity, context, propType);
            association.setAssociatedEntity(associatedEntity);
            boolean isNotCircular = entity != associatedEntity;
            if (relatedClassPropertyName != null && isNotCircular) {
                association.setReferencedPropertyName(relatedClassPropertyName);
            }
        }

        return association;
    }

    /**
     * check if mappedBy is set explicitly to null for the given property.
     * @param property
     * @param mappedBy
     * @return true if mappedBy is set explicitly to null
     */
    private boolean forceUnidirectional(PropertyDescriptor property, Map mappedBy) {
        return mappedBy.containsKey(property.getName()) && (mappedBy.get(property.getName())==null);
    }

    /**
     * Tries to obtain or create an associated entity. Note that if #canExpandMappingContext is set to false then this method may return null
     *
     * @param entity The main entity
     * @param context The context
     * @param propType The associated property type
     * @return The associated entity or null
     */
    protected PersistentEntity getOrCreateAssociatedEntity(PersistentEntity entity, MappingContext context, Class propType) {
        PersistentEntity associatedEntity = context.getPersistentEntity(propType.getName());
        if (associatedEntity == null) {
            if(canExpandMappingContext) {
                if (entity.isExternal()) {
                    associatedEntity = context.addExternalPersistentEntity(propType);
                }
                else {
                    associatedEntity = context.addPersistentEntity(propType);
                }
            }
        }
        else {
            if(!associatedEntity.isInitialized()) {
                associatedEntity.initialize();
            }
        }
        return associatedEntity;
    }

    /**
     * Tries to obtain or create an embedded entity. Note that if #canExpandMappingContext is set to false then this method may return null
     *
     * @param entity The main entity
     * @param context The context
     * @param type The associated property type
     * @return The associated entity or null
     */
    protected PersistentEntity getOrCreateEmbeddedEntity(PersistentEntity entity, MappingContext context, Class type) {
        PersistentEntity associatedEntity = context.getPersistentEntity(type.getName());
        if (associatedEntity == null) {
            // If this is a persistent entity, add and initialize, otherwise
            // assume it's embedded
            if( isPersistentEntity(type) ) {
                if (entity.isExternal()) {
                    associatedEntity = context.addExternalPersistentEntity(type);
                    associatedEntity.initialize();
                }
                else {
                    associatedEntity = context.addPersistentEntity(type);
                    associatedEntity.initialize();
                }
            }
            else {
                PersistentEntity embeddedEntity = context.createEmbeddedEntity(type);
                embeddedEntity.initialize();
                return embeddedEntity;
            }
        }
        else {
            if(!associatedEntity.isInitialized()) {
                associatedEntity.initialize();
            }
        }
        return associatedEntity;
    }

    private boolean isNotMappedToDifferentProperty(PropertyDescriptor property,
            String relatedClassPropertyName, Map mappedBy) {

        String mappedByForRelation = (String)mappedBy.get(relatedClassPropertyName);
        if (mappedByForRelation == null) return true;
        if (!property.getName().equals(mappedByForRelation)) return false;
        return true;
    }

    private String findOneToManyThatMatchesType(PersistentEntity entity, Map relatedClassRelationships) {
        for (Object o : relatedClassRelationships.keySet()) {
            String currentKey = (String) o;
            Class<?> currentClass = (Class<?>)relatedClassRelationships.get(currentKey);

            if (currentClass.isAssignableFrom(entity.getJavaClass())) {
                return currentKey;
            }
        }

        return null;
    }

    private boolean isCollectionType(Class type) {
        return Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type);
    }

    private boolean isExcludedProperty(String propertyName, ClassMapping classMapping, Collection transients) {
        IdentityMapping id = classMapping != null ? classMapping.getIdentifier() : null;
        String[] identifierName = id != null ? id.getIdentifierName() : null;
        return identifierName != null && propertyName.equals(identifierName[0]) ||
                id == null && propertyName.equals(IDENTITY_PROPERTY) ||
                EXCLUDED_PROPERTIES.contains(propertyName) ||
                transients.contains(propertyName);
    }

    /**
     * Retrieves the association map
     * @param cpf The ClassPropertyFetcher instance
     * @return The association map
     */
    protected Map getAssociationMap(ClassPropertyFetcher cpf) {
        return getAssociationMap(cpf, HAS_MANY);
    }

    /**
     * Retrieves the association map
     * @param cpf The ClassPropertyFetcher instance
     * @return The association map
     */
    protected Map getAllAssociationMap(ClassPropertyFetcher cpf) {

        Map associationMap = getAssociationMap(cpf, HAS_MANY);
        associationMap.putAll(getAssociationMap(cpf, HAS_ONE));
        associationMap.putAll(getAssociationMap(cpf, BELONGS_TO));
        return associationMap;
    }

    private Map getAssociationMap(ClassPropertyFetcher cpf, String relationshipType) {
        return getMapStaticProperty(cpf, relationshipType);
    }

    private PersistentEntity getPersistentEntity(Class javaClass, MappingContext context, ClassMapping classMapping) {
        if (classMapping != null) {
            return classMapping.getEntity();
        }

        return context.getPersistentEntity(javaClass.getName());
    }

    public Set getOwningEntities(Class javaClass, MappingContext context) {
        return establishRelationshipOwners(ClassPropertyFetcher.forClass(javaClass));
    }

    /**
     * @see org.grails.datastore.mapping.model.MappingConfigurationStrategy#getIdentity(Class, org.grails.datastore.mapping.model.MappingContext)
     */
    public PersistentProperty getIdentity(Class javaClass, MappingContext context) {
        ClassPropertyFetcher cpf = ClassPropertyFetcher.forClass(javaClass);
        PersistentEntity entity = context.getPersistentEntity(javaClass.getName());
        ClassMapping mapping = entity.getMapping();

        IdentityMapping id = mapping.getIdentifier();
        final String[] names = id.getIdentifierName();
        if (names.length == 1) {
            final PropertyDescriptor pd = cpf.getPropertyDescriptor(names[0]);

            if (pd != null) {
                return propertyFactory.createIdentity(entity, context, pd);
            }
            if (!entity.isExternal()) {
                throw new IllegalMappingException("Mapped identifier [" + names[0] + "] for class [" +
                      javaClass.getName() + "] is not a valid property");
            }
            return null;
        }
        return null;
    }

    /**
     * Obtains the identity mapping for the specified class mapping
     *
     * @param classMapping The class mapping
     * @return The identity mapping
     */
    public IdentityMapping getIdentityMapping(ClassMapping classMapping) {
        return propertyFactory.createIdentityMapping(classMapping);
    }

    public IdentityMapping getDefaultIdentityMapping(final ClassMapping classMapping) {
        return propertyFactory.createDefaultIdentityMapping(classMapping);
    }
}
