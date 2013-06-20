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

package org.grails.datastore.mapping.config;

import groovy.lang.Closure;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.grails.datastore.mapping.config.groovy.MappingConfigurationBuilder;
import org.grails.datastore.mapping.model.ClassMapping;
import org.grails.datastore.mapping.model.IdentityMapping;
import org.grails.datastore.mapping.model.MappingFactory;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.config.GormProperties;
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher;
import org.springframework.beans.BeanUtils;

/**
 * Abstract GORM implementation that uses the GORM MappingConfigurationBuilder to configure entity mappings.
 *
 * @author Graeme Rocher
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class AbstractGormMappingFactory<R extends Entity, T extends Property> extends MappingFactory<R, T> {

    protected Map<PersistentEntity, Map<String, T>> entityToPropertyMap = new HashMap<PersistentEntity, Map<String, T>>();
    private Closure defaultMapping;

    @Override
    public R createMappedForm(PersistentEntity entity) {
        ClassPropertyFetcher cpf = ClassPropertyFetcher.forClass(entity.getJavaClass());
        R family = BeanUtils.instantiate(getEntityMappedFormType());
        MappingConfigurationBuilder builder = new MappingConfigurationBuilder(family, getPropertyMappedFormType());

        if (defaultMapping != null) {
            builder.evaluate(defaultMapping);
        }
        List<Closure> values = cpf.getStaticPropertyValuesFromInheritanceHierarchy(GormProperties.MAPPING, Closure.class);
        for (int i = values.size(); i > 0; i--) {
            Closure value = values.get(i - 1);
            builder.evaluate(value);
        }
        values = cpf.getStaticPropertyValuesFromInheritanceHierarchy(GormProperties.CONSTRAINTS, Closure.class);
        for (int i = values.size(); i > 0; i--) {
            Closure value = values.get(i - 1);
            builder.evaluate(value);
        }
        entityToPropertyMap.put(entity, builder.getProperties());
        return family;
    }

    public void setDefaultMapping(Closure defaultMapping) {
        this.defaultMapping = defaultMapping;
    }

    protected abstract Class<T> getPropertyMappedFormType();

    protected abstract Class<R> getEntityMappedFormType();

    @Override
    public IdentityMapping createIdentityMapping(ClassMapping classMapping) {
        Map<String, T> props = entityToPropertyMap.get(classMapping.getEntity());
        if (props != null) {
            T property  = props.get(IDENTITY_PROPERTY);
            IdentityMapping customIdentityMapping = getIdentityMappedForm(classMapping,property);
            if (customIdentityMapping != null) {
                return customIdentityMapping;
            }
        }
        return super.createIdentityMapping(classMapping);
    }

    protected IdentityMapping getIdentityMappedForm(ClassMapping classMapping, T property) {
        return null;
    }

    @Override
    public T createMappedForm(PersistentProperty mpp) {
        Map<String, T> properties = entityToPropertyMap.get(mpp.getOwner());
        if (properties != null && properties.containsKey(mpp.getName())) {
            return properties.get(mpp.getName());
        }
        else if (properties != null) {
            Property property  = properties.get(IDENTITY_PROPERTY);
            if (property != null && mpp.getName().equals(property.getName())) {
                return (T) property;
            }
        }

        T defaultMapping = properties != null ? properties.get("*") : null;
        if (defaultMapping != null) {
            try {
                return (T) defaultMapping.clone();
            } catch (CloneNotSupportedException e) {
                return BeanUtils.instantiate(getPropertyMappedFormType());
            }
        }
        else {
            return BeanUtils.instantiate(getPropertyMappedFormType());
        }
    }
}
