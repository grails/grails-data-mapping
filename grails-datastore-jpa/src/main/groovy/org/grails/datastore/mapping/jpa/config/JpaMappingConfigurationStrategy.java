/* Copyright (C) 2011 SpringSource
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
package org.grails.datastore.mapping.jpa.config;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.grails.datastore.mapping.model.ClassMapping;
import org.grails.datastore.mapping.model.IdentityMapping;
import org.grails.datastore.mapping.model.MappingConfigurationStrategy;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.MappingFactory;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.types.ToOne;
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher;

/**
 * Configuration strategy for JPA.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class JpaMappingConfigurationStrategy implements MappingConfigurationStrategy{

    private MappingFactory propertyFactory;
    private Map<Class, PersistentProperty> identities = new HashMap<Class, PersistentProperty>();
    private Map<Class, List<PersistentProperty>> properties= new HashMap<Class, List<PersistentProperty>>();
    private Map<Class, Set> owningEntities = new HashMap<Class, Set>();

    public JpaMappingConfigurationStrategy(MappingFactory propertyFactory) {
        this.propertyFactory = propertyFactory;
    }

    public boolean isPersistentEntity(Class javaClass) {
        return javaClass != null && javaClass.getAnnotation(Entity.class) != null;
    }

    public List<PersistentProperty> getPersistentProperties(Class javaClass,
            MappingContext context) {
        return getPersistentProperties(javaClass, context, null);
    }

    private PersistentEntity getPersistentEntity(Class javaClass, MappingContext context, ClassMapping classMapping) {
        PersistentEntity entity;
        if (classMapping != null) {
            entity = classMapping.getEntity();
        }
        else {
            entity = context.getPersistentEntity(javaClass.getName());
        }
        return entity;
    }

    public List<PersistentProperty> getPersistentProperties(Class javaClass,
            MappingContext context, ClassMapping mapping) {
        initializeClassMapping(javaClass, context, mapping);
        return properties.get(javaClass);
    }

    public void initializeClassMapping(Class javaClass, MappingContext context,
            ClassMapping mapping) {
        if (properties.containsKey(javaClass)) {
            return;
        }

        List<PersistentProperty> persistentProperties = new ArrayList<PersistentProperty>();
        Set<Class> owners = new HashSet<Class>();

        properties.put(javaClass, persistentProperties);
        owningEntities.put(javaClass, owners);

        final ClassPropertyFetcher cpf = ClassPropertyFetcher.forClass(javaClass);
        final PersistentEntity owner = getPersistentEntity(javaClass, context, mapping);

        for (PropertyDescriptor propertyDescriptor : cpf.getPropertyDescriptors()) {
            if (propertyDescriptor.getReadMethod() != null && propertyDescriptor.getWriteMethod() != null) {
                Field field;
                try {
                    field = cpf.getDeclaredField(propertyDescriptor.getName());
                } catch (Exception e) {
                    continue;
                }
                if (field != null) {
                    if (field.getAnnotation(Basic.class) != null || field.getAnnotation(Temporal.class) != null || field.getAnnotation(Version.class) != null) {
                        persistentProperties.add(
                                propertyFactory.createSimple(owner, context, propertyDescriptor)
                        );
                    }
                    else if (field.getAnnotation(Id.class) != null) {
                        identities.put(javaClass, propertyFactory.createIdentity(owner, context, propertyDescriptor));
                    }
                    else if (field.getAnnotation(Embedded.class) != null) {
                        final org.grails.datastore.mapping.model.types.Embedded embeddedProperty = propertyFactory.createEmbedded(owner, context, propertyDescriptor);
                        embeddedProperty.setAssociatedEntity(getOrCreateAssociatedEntity(context, field.getType()));
                        persistentProperties.add(
                                embeddedProperty
                        );
                    }
                    else if (field.getAnnotation(OneToOne.class) != null) {
                        OneToOne one2one = field.getAnnotation(OneToOne.class);

                        if (one2one.mappedBy() != null && one2one.targetEntity() != null) {
                            owners.add(one2one.targetEntity());
                        }
                        final ToOne oneToOneProperty = propertyFactory.createOneToOne(owner, context, propertyDescriptor);
                        oneToOneProperty.setAssociatedEntity(getOrCreateAssociatedEntity(context, field.getType()));
                        persistentProperties.add(
                                oneToOneProperty
                        );
                    }
                    else if (field.getAnnotation(OneToMany.class) != null) {
                        OneToMany one2m = field.getAnnotation(OneToMany.class);

                        if (one2m.mappedBy() != null && one2m.targetEntity() != null) {
                            owners.add(one2m.targetEntity());
                        }
                        final org.grails.datastore.mapping.model.types.OneToMany oneToManyProperty = propertyFactory.createOneToMany(owner, context, propertyDescriptor);
                        oneToManyProperty.setAssociatedEntity(getOrCreateAssociatedEntity(context, one2m.targetEntity()));
                        persistentProperties.add(
                                oneToManyProperty
                        );
                    }
                    else if (field.getAnnotation(ManyToMany.class) != null) {
                        ManyToMany m2m = field.getAnnotation(ManyToMany.class);

                        if (m2m.mappedBy() != null && m2m.targetEntity() != null) {
                            owners.add(m2m.targetEntity());
                        }
                        final org.grails.datastore.mapping.model.types.ManyToMany manyToManyProperty = propertyFactory.createManyToMany(owner, context, propertyDescriptor);
                        manyToManyProperty.setAssociatedEntity(getOrCreateAssociatedEntity(context, m2m.targetEntity()));
                        persistentProperties.add(
                                manyToManyProperty
                        );
                    }
                    else if (field.getAnnotation(ManyToOne.class) != null) {
                        final ToOne manyToOneProperty = propertyFactory.createManyToOne(owner, context, propertyDescriptor);
                        manyToOneProperty.setAssociatedEntity(getOrCreateAssociatedEntity(context, field.getType()));
                        persistentProperties.add(
                                manyToOneProperty
                        );
                    }
                    else if (field.getAnnotation(Transient.class) == null){
                        persistentProperties.add(
                                propertyFactory.createSimple(owner, context, propertyDescriptor)
                        );
                    }
                }
            }
        }
    }

    private PersistentEntity getOrCreateAssociatedEntity(MappingContext context, Class propType) {
        PersistentEntity associatedEntity = context.getPersistentEntity(propType.getName());
        if (associatedEntity == null) {
            associatedEntity = context.addPersistentEntity(propType);
        }
        return associatedEntity;
    }

    public PersistentProperty getIdentity(Class javaClass, MappingContext context) {
        initializeClassMapping(javaClass, context, null);
        return identities.get(javaClass);
    }

    /**
     * Obtains the identity mapping for the specified class mapping
     *
     * @param classMapping The class mapping
     * @return The identity mapping
     */
    @Override
    public IdentityMapping getIdentityMapping(ClassMapping classMapping) {
        return getDefaultIdentityMapping(classMapping);
    }

    public IdentityMapping getDefaultIdentityMapping(ClassMapping classMapping) {
        return null;
    }

    public Set getOwningEntities(Class javaClass, MappingContext context) {
        initializeClassMapping(javaClass, context, null);
        final Set set = owningEntities.get(javaClass);
        if (set != null) {
            return set;
        }
        return Collections.EMPTY_SET;
    }
}
