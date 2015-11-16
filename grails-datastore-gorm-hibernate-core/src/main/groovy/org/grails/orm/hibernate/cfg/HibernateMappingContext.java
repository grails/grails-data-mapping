/*
 * Copyright 2015 original authors
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
package org.grails.orm.hibernate.cfg;

import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import org.grails.datastore.gorm.GormEntity;
import org.grails.datastore.mapping.config.AbstractGormMappingFactory;
import org.grails.datastore.mapping.config.Property;
import org.grails.datastore.mapping.config.groovy.MappingConfigurationBuilder;
import org.grails.datastore.mapping.model.*;
import org.grails.datastore.mapping.model.config.GormMappingConfigurationStrategy;
import org.grails.datastore.mapping.model.config.GormProperties;
import org.grails.orm.hibernate.AbstractHibernateDatastore;
import org.springframework.core.env.PropertyResolver;
import org.springframework.validation.Errors;

import java.lang.annotation.Annotation;

/**
 * A Mapping context for Hibernate
 *
 * @author Graeme Rocher
 * @since 5.0
 */
public class HibernateMappingContext extends AbstractMappingContext {

    private static final String[] DEFAULT_IDENTITY_MAPPING = new String[] {GormProperties.IDENTITY};
    private final HibernateMappingFactory mappingFactory;
    private final MappingConfigurationStrategy syntaxStrategy;

    public HibernateMappingContext(PropertyResolver configuration, Object contextObject, Class...persistentClasses) {
        this(configuration.getProperty(AbstractHibernateDatastore.CONFIG_PROPERTY_DEFAULT_MAPPING, Closure.class, null), contextObject);
        addPersistentEntities(persistentClasses);
    }

    public HibernateMappingContext(PropertyResolver configuration, Object contextObject) {
        this(configuration.getProperty(AbstractHibernateDatastore.CONFIG_PROPERTY_DEFAULT_MAPPING, Closure.class, null), contextObject);
    }

    public HibernateMappingContext() {
        this(null);
    }
    public HibernateMappingContext(Closure defaultMapping) {
        this(defaultMapping, null);
    }
    public HibernateMappingContext(Closure defaultMapping, Object contextObject) {
        this.mappingFactory = new HibernateMappingFactory();
        if(defaultMapping != null) {
            this.mappingFactory.setDefaultMapping(defaultMapping);
        }
        this.mappingFactory.setContextObject(contextObject);
        this.syntaxStrategy = new GormMappingConfigurationStrategy(mappingFactory) {
            @Override
            protected boolean supportsCustomType(Class<?> propertyType) {
                return !Errors.class.isAssignableFrom(propertyType);
            }
        };
    }



    @Override
    public MappingConfigurationStrategy getMappingSyntaxStrategy() {
        return syntaxStrategy;
    }

    @Override
    public MappingFactory getMappingFactory() {
        return mappingFactory;
    }

    @Override
    protected PersistentEntity createPersistentEntity(Class javaClass) {
        if(GormEntity.class.isAssignableFrom(javaClass) || doIsDomainClassCheck(javaClass)) {
            return new HibernatePersistentEntity(javaClass, this);
        }
        return null;
    }

    public static boolean isDomainClass(Class clazz) {
        return doIsDomainClassCheck(clazz);
    }

    private static boolean doIsDomainClassCheck(Class<?> clazz) {
        // it's not a closure
        if (Closure.class.isAssignableFrom(clazz)) {
            return false;
        }

        if (clazz.isEnum()) return false;

        Annotation[] allAnnotations = clazz.getAnnotations();
        for (Annotation annotation : allAnnotations) {
            String annName = annotation.annotationType().getName();
            if (annName.equals("grails.persistence.Entity")) {
                return true;
            }
            if (annName.equals("grails.gorm.annotation.Entity")) {
                return true;
            }
        }

        Class<?> testClass = clazz;
        while (testClass != null && !testClass.equals(GroovyObject.class) && !testClass.equals(Object.class)) {
            try {
                // make sure the identify and version field exist
                testClass.getDeclaredField(GormProperties.IDENTITY);
                testClass.getDeclaredField(GormProperties.VERSION);

                // passes all conditions return true
                return true;
            }
            catch (SecurityException e) {
                // ignore
            }
            catch (NoSuchFieldException e) {
                // ignore
            }
            testClass = testClass.getSuperclass();
        }

        return false;
    }

    @Override
    public PersistentEntity createEmbeddedEntity(Class type) {
        return new HibernateEmbeddedPersistentEntity(type, this);
    }

    static class HibernateEmbeddedPersistentEntity extends EmbeddedPersistentEntity {
        private final ClassMapping<Mapping> classMapping;
        public HibernateEmbeddedPersistentEntity(Class type, MappingContext ctx) {
            super(type, ctx);
            this.classMapping = new ClassMapping<Mapping>() {
                Mapping mappedForm = (Mapping) context.getMappingFactory().createMappedForm(HibernateEmbeddedPersistentEntity.this);
                @Override
                public PersistentEntity getEntity() {
                    return HibernateEmbeddedPersistentEntity.this;
                }

                @Override
                public Mapping getMappedForm() {
                    return mappedForm;
                }

                @Override
                public IdentityMapping getIdentifier() {
                    return null;
                }
            };
        }

        @Override
        public ClassMapping getMapping() {
            return classMapping;
        }
    }

    class HibernateMappingFactory extends AbstractGormMappingFactory<Mapping,PropertyConfig> {

        public HibernateMappingFactory() {
        }

        @Override
        protected MappingConfigurationBuilder createConfigurationBuilder(PersistentEntity entity, Mapping mapping) {
            return new HibernateMappingBuilder(mapping, entity.getName());
        }

        @Override
        public IdentityMapping createIdentityMapping(final ClassMapping classMapping) {
            final Mapping mappedForm = createMappedForm(classMapping.getEntity());
            final Object identity = mappedForm.getIdentity();
            return new IdentityMapping() {
                @Override
                public String[] getIdentifierName() {
                    if(identity instanceof Identity) {
                        final String name = ((Identity) identity).getName();
                        if(name != null) {
                            return new String[]{name};
                        }
                        else {
                            return DEFAULT_IDENTITY_MAPPING;
                        }
                    }
                    else if(identity instanceof CompositeIdentity) {
                        return ((CompositeIdentity) identity).getPropertyNames();
                    }
                    return DEFAULT_IDENTITY_MAPPING;
                }

                @Override
                public ClassMapping getClassMapping() {
                    return classMapping;
                }

                @Override
                public Property getMappedForm() {
                    return (Property) identity;
                }
            };
        }

        @Override
        protected boolean allowArbitraryCustomTypes() {
            return true;
        }

        @Override
        protected Class<PropertyConfig> getPropertyMappedFormType() {
            return PropertyConfig.class;
        }

        @Override
        protected Class<Mapping> getEntityMappedFormType() {
            return Mapping.class;
        }
    }
}
