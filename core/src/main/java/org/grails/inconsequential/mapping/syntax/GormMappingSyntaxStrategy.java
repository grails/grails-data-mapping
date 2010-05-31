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
package org.grails.inconsequential.mapping.syntax;

import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import org.grails.inconsequential.mapping.*;
import static org.grails.inconsequential.mapping.syntax.GormProperties.*;

import org.grails.inconsequential.mapping.types.Simple;
import org.grails.inconsequential.reflect.ClassPropertyFetcher;

import javax.persistence.Entity;
import java.beans.PropertyDescriptor;
import java.util.*;

/**
 * <p>This implementation of the MappingSyntaxStrategy interface
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
public class GormMappingSyntaxStrategy implements MappingSyntaxStrategy {
    private static final String IDENTITY_PROPERTY = "id";
    private static final String VERSION_PROPERTY = "version";
    private MappingFactory propertyFactory;
    private static final Set EXCLUDED_PROPERTIES = new HashSet() {{
        add("class"); add("metaClass");
    }};

    public GormMappingSyntaxStrategy(MappingFactory propertyFactory) {
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
            Set owners = tmp != null ? new HashSet(tmp) : Collections.emptySet();
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
                if (Collection.class.isAssignableFrom(currentPropType) || Map.class.isAssignableFrom(currentPropType)) {
//                    TODO: Implement mapping of collection types
//                    establishRelationshipForCollection(descriptor, entity, context);
                }
                // otherwise if the type is a domain class establish relationship
                else if (isPersistentEntity(currentPropType)) {
//                      TODO: Implement mapping of association types
//                    establishDomainClassRelationship(descriptor, entity, context);
                }
                else {
                    if (embedded.contains(propertyName)) {
                        // TODO: Implement mapping of embedded types
    //                    establishDomainClassRelationship(descriptor, entity, context);
                    }
                    else if(MappingFactory.isSimpleType(descriptor.getPropertyType())) {
                        persistentProperties.add(propertyFactory.createSimple(entity, context, descriptor));
                    }
                }

            }
        }
        return persistentProperties;
    }

    private boolean isExcludedProperty(String propertyName, ClassMapping classMapping, Collection transients) {
        IdentityMapping id = classMapping != null ? classMapping.getIdentifier() : null;
        if(id != null && id.getIdentifierName()[0].equals(propertyName)) return true;
        else return id == null && propertyName.equals(IDENTITY_PROPERTY) || EXCLUDED_PROPERTIES.contains(propertyName) || transients.contains(propertyName);
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

    /**
     * @see org.grails.inconsequential.mapping.MappingSyntaxStrategy#getIdentity(Class, org.grails.inconsequential.mapping.MappingContext)
     */
    public PersistentProperty getIdentity(Class javaClass, MappingContext context) {
        ClassPropertyFetcher cpf = ClassPropertyFetcher.forClass(javaClass);
        PersistentEntity entity = context.getPersistentEntity(javaClass.getName());
        if(entity instanceof MappedPersistentEntity) {
            ClassMapping mapping = ((MappedPersistentEntity) entity).getMapping();

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
        else {
            final PropertyDescriptor pd = cpf.getPropertyDescriptor(IDENTITY_PROPERTY);

            if(pd != null) {
                return propertyFactory.createIdentity(entity, context, pd);
            }
            else {
                throw new IllegalMappingException("Persistent class ["+javaClass.getName()+"] does not have an 'id' property nor does it specify an alternative property to use as an identifier. Either create an 'id' property or specified which property is the 'id' via the mapping.");
            }

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
