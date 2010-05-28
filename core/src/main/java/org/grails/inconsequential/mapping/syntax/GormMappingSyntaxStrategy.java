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
import org.grails.inconsequential.reflect.ClassPropertyFetcher;

import javax.persistence.Entity;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>This implementation of the MappingSyntaxStrategy interface
 * will interpret GORM-style syntax for defining entities and associations</p>
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class GormMappingSyntaxStrategy implements MappingSyntaxStrategy {
    private static final String IDENTITY_PROPERTY = "id";
    private static final String VERSION_PROPERTY = "version";
    private MappingFactory propertyFactory;

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

    public List<PersistentProperty> getPersistentProperties(Class javaClass, MappingContext context, ClassMapping mapping) {
        // TODO: Migrate logic from DefaultGrailsDomainClass to here to GORM syntax is portable outside of Grails
        return new ArrayList<PersistentProperty>();
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
