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
package org.springframework.datastore.mapping.model.config;

import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import org.springframework.datastore.mapping.model.*;
import org.springframework.datastore.mapping.model.types.Association;
import org.springframework.datastore.mapping.model.types.OneToOne;
import org.springframework.datastore.mapping.model.types.ToOne;
import org.springframework.datastore.mapping.reflect.ClassPropertyFetcher;
import org.springframework.datastore.mapping.reflect.ReflectionUtils;
import org.springframework.util.StringUtils;

import javax.persistence.Entity;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.util.*;

import static org.springframework.datastore.mapping.model.config.GormProperties.*;

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
public class GormMappingConfigurationStrategy implements MappingConfigurationStrategy {
    private static final String IDENTITY_PROPERTY = "id";
    private static final String VERSION_PROPERTY = "version";
    private MappingFactory propertyFactory;
    private static final Set EXCLUDED_PROPERTIES = new HashSet() {{
        add("class"); add("metaClass");
    }};

    public GormMappingConfigurationStrategy(MappingFactory propertyFactory) {
        super();
        this.propertyFactory = propertyFactory;
    }

    /**
     * Tests whether an class is a persistent entity
     *
     * Based on the same method in Grails core within the DomainClassArtefactHandler class
     * @param clazz The java class
     *
     * @return True if it is a persistent entity
     */
    @SuppressWarnings({"unchecked"})
    public boolean isPersistentEntity(Class clazz) {
        // its not a closure
        if(clazz == null)return false;
        if(Closure.class.isAssignableFrom(clazz)) {
            return false;
        }
        if(Enum.class.isAssignableFrom(clazz)) return false;
        if(clazz.getAnnotation(Entity.class)!=null) {
            return true;
        }
        final Annotation[] annotations = clazz.getAnnotations();
        // this is done so we don't need a statically typed reference to the Grails annotation
        for (Annotation annotation : annotations) {
            if(annotation.toString().equals("@grails.persistence.Entity()")) return true;
        }
        Class testClass = clazz;
        boolean result = false;
        while(testClass!=null&&!testClass.equals(GroovyObject.class)&&!testClass.equals(Object.class)) {
            try {
                // make sure the identify and version field exist
                testClass.getDeclaredField( IDENTITY_PROPERTY );
                testClass.getDeclaredField( VERSION_PROPERTY );

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

        final ArrayList<PersistentProperty> persistentProperties = new ArrayList<PersistentProperty>();

        if(entity != null) {
            ClassPropertyFetcher cpf = ClassPropertyFetcher.forClass(entity.getJavaClass());

            Collection tmp = cpf.getStaticPropertyValue(BELONGS_TO, Collection.class);
            // owners are the classes that own this class
            Collection embedded = cpf.getStaticPropertyValue(EMBEDDED, Collection.class);
            if(embedded == null) embedded = Collections.emptyList();

            Collection transients = cpf.getStaticPropertyValue(TRANSIENT, Collection.class);
            if(transients == null) transients = Collections.emptyList();

            // hasMany associations for defining one-to-many and many-to-many
            Map hasManyMap = getAssociationMap(cpf);
            // mappedBy for defining by which property an association is mapped
            Map mappedByMap = cpf.getStaticPropertyValue(MAPPED_BY, Map.class);
            if(mappedByMap == null) mappedByMap = Collections.emptyMap();
            // hasOne for declaring a one-to-one association with the foreign key in the child
            Map hasOneMap = cpf.getStaticPropertyValue(HAS_ONE, Map.class);
            if(hasOneMap == null) hasOneMap = Collections.emptyMap();



            PropertyDescriptor[] descriptors = cpf.getPropertyDescriptors();

            for (PropertyDescriptor descriptor : descriptors) {
                final String propertyName = descriptor.getName();
                if(isExcludedProperty(propertyName, classMapping, transients)) continue;
                Class currentPropType = descriptor.getPropertyType();
                PersistentProperty prop;
                // establish if the property is a one-to-many
                // if it is a Set and there are relationships defined
                // and it is defined as persistent
                if (isCollectionType(currentPropType)) {
                    final Association association = establishRelationshipForCollection(descriptor, entity, context, hasManyMap, mappedByMap);
                    if(association != null) {
                        configureOwningSide(association);
                        persistentProperties.add(association);
                    }
                }
                else if (embedded.contains(propertyName)) {
                	ToOne association = propertyFactory.createEmbedded(entity, context, descriptor);
                	PersistentEntity associatedEntity = getOrCreateAssociatedEntity(context, association.getType());
                	association.setAssociatedEntity(associatedEntity);
                	persistentProperties.add(association);
                }
                // otherwise if the type is a domain class establish relationship
                else if (isPersistentEntity(currentPropType)) {
                    final ToOne association = establishDomainClassRelationship(entity, descriptor, context, hasOneMap);
                    if(association != null) {
                        configureOwningSide(association);
                        persistentProperties.add(association);
                    }
                }
                else if(MappingFactory.isSimpleType(descriptor.getPropertyType())) {
                    persistentProperties.add( propertyFactory.createSimple(entity, context, descriptor) );
                }

            }
        }
        return persistentProperties;
    }

    private void configureOwningSide(Association association) {
        if(association.isBidirectional()) {
            if(association.getAssociatedEntity().isOwningEntity(association.getOwner())) {
                association.setOwningSide(true);
            }
        }
        else {
            if(association instanceof OneToOne) {
                association.setOwningSide(true);
            } else {
               if(association.getAssociatedEntity().isOwningEntity(association.getOwner())) {
                   association.setOwningSide(true);
               }
               else {
                   association.setOwningSide(false);
               }
            }
        }
    }

    /**
     * Evaluates the belongsTo property to find out who owns who
     */
    @SuppressWarnings("unchecked")
    private Set establishRelationshipOwners(ClassPropertyFetcher cpf) {
        Set owners = null;
        Class<?> belongsTo = cpf.getStaticPropertyValue(BELONGS_TO, Class.class);
        if (belongsTo == null) {
            List ownersProp = cpf.getStaticPropertyValue(BELONGS_TO, List.class);
            if (ownersProp != null) {
                owners = new HashSet(ownersProp);
            }
            else {
                Map ownersMap = cpf.getStaticPropertyValue(BELONGS_TO, Map.class);
                if (ownersMap!=null) {
                    owners = new HashSet(ownersMap.values());
                }
            }
        }
        else {
            owners = new HashSet();
            owners.add(belongsTo);
        }
        if(owners == null) owners = Collections.emptySet();
        return owners;
    }

    private Association establishRelationshipForCollection(PropertyDescriptor property, PersistentEntity entity, MappingContext context, Map<String, Class> hasManyMap, Map mappedByMap) {
        // is it a relationship
        Class relatedClassType = hasManyMap.get(property.getName());

        Association association = null;

        if (relatedClassType != null) {
            // set the referenced type in the property
            ClassPropertyFetcher cpf = ClassPropertyFetcher.forClass(relatedClassType);
            String referencedPropertyName = null;

            // if the related type is a domain class
            // then figure out what kind of relationship it is
            if (isPersistentEntity(relatedClassType)) {

                // check the relationship defined in the referenced type
                // if it is also a Set/domain class etc.
                Map relatedClassRelationships = cpf.getPropertyValue(HAS_MANY, Map.class);
                Class<?> relatedClassPropertyType = null;

                // First check whether there is an explicit relationship
                // mapping for this property (as provided by "mappedBy").
                String mappingProperty = (String)mappedByMap.get(property.getName());
                if ( StringUtils.hasText(mappingProperty)) {
                    // First find the specified property on the related class, if it exists.
                    PropertyDescriptor pd = findProperty(cpf.getPropertiesOfType(entity.getJavaClass()), mappingProperty);

                    // If a property of the required type does not exist, search
                    // for any collection properties on the related class.
                    if (pd == null) pd = findProperty(cpf.getPropertiesAssignableToType(Collection.class), mappingProperty);

                    // We've run out of options. The given "mappedBy"
                    // setting is invalid.
                    if (pd == null) {
                        throw new IllegalMappingException("Non-existent mapping property ["+mappingProperty+"] specified for property ["+property.getName()+"] in class ["+entity.getJavaClass()+"]");
                    }

                    // Tie the properties together.
                    relatedClassPropertyType = pd.getPropertyType();
                    referencedPropertyName = pd.getName();
                }
                else {
                    // if the related type has a relationships map it may be a many-to-many
                    // figure out if there is a many-to-many relationship defined
                    if (isRelationshipManyToMany(entity, relatedClassType, relatedClassRelationships)) {
                        String relatedClassPropertyName = null;
                        Map relatedClassMappedBy = cpf.getStaticPropertyValue(MAPPED_BY, Map.class);
                        if(relatedClassMappedBy == null) relatedClassMappedBy = Collections.emptyMap();
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

                        // if there is one defined get the type
                        if (relatedClassPropertyName != null) {
                            relatedClassPropertyType = cpf.getPropertyType(relatedClassPropertyName);
                        }
                    }
                    // otherwise figure out if there is a one-to-many relationship by retrieving any properties that are of the related type
                    // if there is more than one property then (for the moment) ignore the relationship
                    if (relatedClassPropertyType == null) {
                        List<PropertyDescriptor> descriptors = cpf.getPropertiesOfType(entity.getJavaClass());

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
                                throw new IllegalMappingException("Property ["+property.getName()+"] in class ["+entity.getJavaClass()+"] is a bidirectional one-to-many with two possible properties on the inverse side. "+
                                        "Either name one of the properties on other side of the relationship ["+classPropertyName+"] or use the 'mappedBy' static to define the property " +
                                        "that the relationship is mapped with. Example: static mappedBy = ["+property.getName()+":'myprop']");
                            }
                            relatedClassPropertyType = pd.getPropertyType();
                            referencedPropertyName = pd.getName();
                        }
                    }
                }

                // if its a many-to-many figure out the owning side of the relationship
                final boolean isInverseSideEntity = isPersistentEntity(relatedClassPropertyType);
                if (relatedClassPropertyType == null || isInverseSideEntity) {
                    // uni-directional one-to-many
                    association = propertyFactory.createOneToMany(entity, context, property);
                }
                else if (Collection.class.isAssignableFrom(relatedClassPropertyType) || Map.class.isAssignableFrom(relatedClassPropertyType) ){
                    // many-to-many
                    association = propertyFactory.createManyToMany(entity, context, property);
                }

                PersistentEntity associatedEntity = getOrCreateAssociatedEntity(context, relatedClassType);

                association.setAssociatedEntity(associatedEntity);
                if(referencedPropertyName != null) {
                    association.setReferencedPropertyName(referencedPropertyName);
                    final PersistentProperty referencedProperty = associatedEntity.getPropertyByName(referencedPropertyName);
                }
            }
            // otherwise set it to not persistent as you can't persist
            // relationships to non-domain classes
            else {
                // TODO: Add support for basic collection types
                //property.setBasicCollectionType(true);
            }
        }
        return association;
    }

    /**
     * Find out if the relationship is a many-to-many
     *
     * @param relatedClassType The related type
     * @param relatedClassRelationships The related types relationships
     * @return <code>true</code> if the relationship is a many-to-many
     */
    @SuppressWarnings("unchecked")
    private boolean isRelationshipManyToMany(PersistentEntity entity,
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
        PropertyDescriptor d = null;
        for (PropertyDescriptor descriptor : descriptors) {
            if (descriptor.getName().equals(propertyName)) {
                d = descriptor;
                break;
            }
        }
        return d;
    }


    /**
     * Establish relationship with related domain class
     *
     * @param entity
     * @param property Establishes a relationship between this class and the domain class property
     * @param context
     * @param hasOneMap
     */
    @SuppressWarnings("unchecked")
    private ToOne establishDomainClassRelationship(PersistentEntity entity, PropertyDescriptor property, MappingContext context, Map hasOneMap) {
        ToOne association = null;
        Class propType = property.getPropertyType();
        ClassPropertyFetcher cpf = ClassPropertyFetcher.forClass(propType);

        // establish relationship to type
        Map relatedClassRelationships = getAssociationMap(cpf);
        @SuppressWarnings("hiding")
        Map mappedBy = cpf.getStaticPropertyValue(MAPPED_BY, Map.class);
        if(mappedBy == null) mappedBy = Collections.emptyMap();

        Class<?> relatedClassPropertyType = null;

        // if there is a relationships map use that to find out
        // whether it is mapped to a Set
        String relatedClassPropertyName = null;
        if (relatedClassRelationships != null && !relatedClassRelationships.isEmpty()) {

            PropertyDescriptor[] descriptors = ReflectionUtils.getPropertiesOfType(entity.getJavaClass(), propType);
            relatedClassPropertyName = findOneToManyThatMatchesType(entity, property, relatedClassRelationships);
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
            PropertyDescriptor[] descriptors = ReflectionUtils.getPropertiesOfType(propType, entity.getJavaClass());

            // if there is only one then the association is established
            if (descriptors.length == 1) {
                relatedClassPropertyType = descriptors[0].getPropertyType();
            }
        }

        //    establish relationship based on this type
        // uni-directional one-to-one

        final boolean isAssociationEntity = isPersistentEntity(relatedClassPropertyType);
        if (relatedClassPropertyType == null || isAssociationEntity) {
            association = propertyFactory.createOneToOne(entity, context, property);

            if (hasOneMap.containsKey(property.getName())) {
                association.setForeignKeyInChild(true);
            }

        }
        // bi-directional many-to-one
        else if (Collection.class.isAssignableFrom(relatedClassPropertyType)||Map.class.isAssignableFrom(relatedClassPropertyType)) {
            association = propertyFactory.createManyToOne(entity, context, property);
        }

        // bi-directional
        if(association != null) {
            PersistentEntity associatedEntity = getOrCreateAssociatedEntity(context, propType);
            association.setAssociatedEntity(associatedEntity);
            if(relatedClassPropertyName != null) {
                association.setReferencedPropertyName(relatedClassPropertyName);
            }
        }

        return association;
    }

    private PersistentEntity getOrCreateAssociatedEntity(MappingContext context, Class propType) {
        PersistentEntity associatedEntity = context.getPersistentEntity(propType.getName());
        if(associatedEntity == null) {
            associatedEntity = context.addPersistentEntity(propType);
        }
        return associatedEntity;
    }


    @SuppressWarnings("unchecked")
    private boolean isNotMappedToDifferentProperty(PropertyDescriptor property,
            String relatedClassPropertyName, @SuppressWarnings("hiding") Map mappedBy) {

        String mappedByForRelation = (String)mappedBy.get(relatedClassPropertyName);
        if (mappedByForRelation == null) return true;
        if (!property.getName().equals(mappedByForRelation)) return false;
        return true;
    }

    private String findOneToManyThatMatchesType(PersistentEntity entity, PropertyDescriptor property, Map relatedClassRelationships) {
        String relatedClassPropertyName = null;

        for (Object o : relatedClassRelationships.keySet()) {
            String currentKey = (String) o;
            Class<?> currentClass = (Class<?>)relatedClassRelationships.get(currentKey);

            if (entity.getName().equals(currentClass.getName())) {
                relatedClassPropertyName = currentKey;
                break;
            }
        }
        return relatedClassPropertyName;
    }


    private boolean isCollectionType(Class type) {
        return Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type);
    }

    private boolean isExcludedProperty(String propertyName, ClassMapping classMapping, Collection transients) {
        IdentityMapping id = classMapping != null ? classMapping.getIdentifier() : null;
        return id != null && id.getIdentifierName()[0].equals(propertyName) || id == null && propertyName.equals(IDENTITY_PROPERTY) || EXCLUDED_PROPERTIES.contains(propertyName) || transients.contains(propertyName);
    }

    /**
     * Retrieves the association map
     * @param cpf The ClassPropertyFetcher instance
     * @return The association map
     */
    protected Map getAssociationMap(ClassPropertyFetcher cpf) {
        Map relationshipMap = cpf.getStaticPropertyValue(HAS_MANY, Map.class);
        if(relationshipMap == null)
            relationshipMap = new HashMap();

        Class theClass = cpf.getJavaClass();
        while(theClass != Object.class) {
            theClass = theClass.getSuperclass();
            ClassPropertyFetcher propertyFetcher = ClassPropertyFetcher.forClass(theClass);
            Map superRelationshipMap = propertyFetcher.getStaticPropertyValue(HAS_MANY, Map.class);
            if(superRelationshipMap != null && !superRelationshipMap.equals(relationshipMap)) {
                relationshipMap.putAll(superRelationshipMap);
            }
        }
        return relationshipMap;
    }


    private PersistentEntity getPersistentEntity(Class javaClass, MappingContext context, ClassMapping classMapping) {
        PersistentEntity entity;
        if(classMapping != null)
            entity = classMapping.getEntity();
        else
            entity = context.getPersistentEntity(javaClass.getName());
        return entity;
    }

    public Set getOwningEntities(Class javaClass, MappingContext context) {
        return establishRelationshipOwners(ClassPropertyFetcher.forClass(javaClass));
    }

    /**
     * @see org.springframework.datastore.mapping.model.MappingConfigurationStrategy#getIdentity(Class, org.springframework.datastore.mapping.model.MappingContext)
     */
    public PersistentProperty getIdentity(Class javaClass, MappingContext context) {
        ClassPropertyFetcher cpf = ClassPropertyFetcher.forClass(javaClass);
        PersistentEntity entity = context.getPersistentEntity(javaClass.getName());
        ClassMapping mapping = entity.getMapping();

        IdentityMapping id = mapping.getIdentifier();
        final String[] names = id.getIdentifierName();
        if(names.length==1) {
            final PropertyDescriptor pd = cpf.getPropertyDescriptor(names[0]);

            if(pd != null) {
                return propertyFactory.createIdentity(entity, context, pd);
            }
            else {
                throw new IllegalMappingException("Mapped identifier ["+names[0]+"] for class ["+javaClass.getName()+"] is not a valid property");
            }

        }
        else {
            // TODO: Support composite / natural identifiers
            throw new UnsupportedOperationException("Mapping of composite identifiers currently not supported");
        }
    }

    public IdentityMapping getDefaultIdentityMapping(final ClassMapping classMapping) {
        return new IdentityMapping() {

            public String[] getIdentifierName() {
                return new String[] { IDENTITY_PROPERTY };
            }

            public ClassMapping getClassMapping() {
                return classMapping;
            }

            public Object getMappedForm() {
                // no custom mapping
                return null;
            }
        };
    }
}
