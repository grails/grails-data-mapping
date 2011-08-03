/* Copyright (C) 2010 SpringSource
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

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.core.annotation.AnnotationUtils;
import org.grails.datastore.mapping.annotation.Entity;
import org.grails.datastore.mapping.annotation.Id;
import org.grails.datastore.mapping.model.ClassMapping;
import org.grails.datastore.mapping.model.IdentityMapping;
import org.grails.datastore.mapping.model.IllegalMappingException;
import org.grails.datastore.mapping.model.MappingConfigurationStrategy;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.MappingFactory;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher;

/**
 * @author Graeme Rocher
 * @since 1.1
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class DefaultMappingConfigurationStrategy implements MappingConfigurationStrategy {

    private static final Set EXCLUDED_PROPERTIES = new HashSet(Arrays.asList("class", "metaClass"));

    private MappingFactory propertyFactory;

    public DefaultMappingConfigurationStrategy(MappingFactory propertyFactory) {
        this.propertyFactory = propertyFactory;
    }

    private boolean isExcludedProperty(String propertyName, ClassMapping classMapping, Collection transients) {
        IdentityMapping id = classMapping != null ? classMapping.getIdentifier() : null;
        return id != null && id.getIdentifierName()[0].equals(propertyName) || EXCLUDED_PROPERTIES.contains(propertyName) || transients.contains(propertyName);
    }

    public boolean isPersistentEntity(Class javaClass) {
        return AnnotationUtils.findAnnotation(javaClass, Entity.class) != null;
    }

    public List<PersistentProperty> getPersistentProperties(Class javaClass, MappingContext context) {
        return getPersistentProperties(javaClass, context, null);
    }

    private PersistentEntity getPersistentEntity(Class javaClass, MappingContext context, ClassMapping classMapping) {
        PersistentEntity entity;
        if (classMapping != null)
            entity = classMapping.getEntity();
        else
            entity = context.getPersistentEntity(javaClass.getName());
        return entity;
    }

    public List<PersistentProperty> getPersistentProperties(Class javaClass, MappingContext context, ClassMapping mapping) {
        PersistentEntity entity = getPersistentEntity(javaClass, context, mapping);

        final ArrayList<PersistentProperty> persistentProperties = new ArrayList<PersistentProperty>();

        if (entity != null) {
            ClassPropertyFetcher cpf = ClassPropertyFetcher.forClass(entity.getJavaClass());
            PropertyDescriptor[] descriptors = cpf.getPropertyDescriptors();

            for (PropertyDescriptor descriptor : descriptors) {
                final String propertyName = descriptor.getName();
                if (isExcludedProperty(propertyName, mapping, Collections.emptyList())) continue;
                Class<?> propertyType = descriptor.getPropertyType();
                if (propertyFactory.isSimpleType(propertyType)) {
                    persistentProperties.add(propertyFactory.createSimple(entity, context, descriptor));
                }
                else if (propertyFactory.isCustomType(propertyType)) {
                    persistentProperties.add(propertyFactory.createCustom(entity, context, descriptor));
                }

            }
        }

        return persistentProperties;
    }

    public PersistentProperty getIdentity(Class javaClass, MappingContext context) {
        final ClassPropertyFetcher cpf = ClassPropertyFetcher.forClass(javaClass);

        for (Field field : cpf.getJavaClass().getDeclaredFields()) {
            final Id annotation = field.getAnnotation(Id.class);
            if (annotation != null) {
                PersistentEntity entity = context.getPersistentEntity(javaClass.getName());
                PropertyDescriptor pd = cpf.getPropertyDescriptor(field.getName());
                return propertyFactory.createIdentity(entity, context, pd);
            }
        }
        throw new IllegalMappingException("No identifier specified for persistent class: " + javaClass.getName());
    }

    public IdentityMapping getDefaultIdentityMapping(final ClassMapping classMapping) {

        final PersistentEntity e = classMapping.getEntity();
        final PersistentProperty identity = getIdentity(e.getJavaClass(), e.getMappingContext());
        return new IdentityMapping() {

            public String[] getIdentifierName() {
                return new String[] { identity.getName() };
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

    public Set getOwningEntities(Class javaClass, MappingContext context) {
        return Collections.emptySet();
    }
}
