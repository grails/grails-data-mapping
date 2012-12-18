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

import java.util.HashMap;
import java.util.Map;

import org.grails.datastore.mapping.model.*;
import org.springframework.beans.BeanUtils;
import org.grails.datastore.mapping.config.groovy.MappingConfigurationBuilder;
import org.grails.datastore.mapping.model.config.GormProperties;
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher;

/**
 * Abstract GORM implementation that uses the GORM MappingConfigurationBuilder to configure entity mappings.
 *
 * @author Graeme Rocher
 */
public abstract class AbstractGormMappingFactory<R, T> extends MappingFactory<R, T> {

    protected Map<PersistentEntity, Map<String, T>> entityToPropertyMap = new HashMap<PersistentEntity, Map<String, T>>();
    private Closure defaultMapping;

    @SuppressWarnings("unchecked")
    @Override
    public R createMappedForm(PersistentEntity entity) {
        ClassPropertyFetcher cpf = ClassPropertyFetcher.forClass(entity.getJavaClass());
        R family = BeanUtils.instantiate(getEntityMappedFormType());
        MappingConfigurationBuilder builder = new MappingConfigurationBuilder(family, getPropertyMappedFormType());

        if(defaultMapping != null) {
            builder.evaluate(defaultMapping);
        }
        Closure value = cpf.getStaticPropertyValue(GormProperties.MAPPING, Closure.class);
        if (value != null) {
            builder.evaluate(value);
        }
        value = cpf.getStaticPropertyValue(GormProperties.CONSTRAINTS, Closure.class);
        if (value != null) {
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
        if(props != null) {
            T property  = props.get(IDENTITY_PROPERTY);
            IdentityMapping customIdentityMapping = getIdentityMappedForm(classMapping,property);
            if(customIdentityMapping != null) {
                return customIdentityMapping;
            }
        }
        return super.createIdentityMapping(classMapping);
    }

    protected IdentityMapping getIdentityMappedForm(ClassMapping classMapping, T property) {
        return null;
    }

    @Override
    public T createMappedForm(@SuppressWarnings("rawtypes") PersistentProperty mpp) {
        Map<String, T> properties = entityToPropertyMap.get(mpp.getOwner());
        if (properties != null && properties.containsKey(mpp.getName())) {
            return properties.get(mpp.getName());
        }
        else if(properties != null) {
            Property property  = (Property) properties.get(IDENTITY_PROPERTY);
            if(property != null && mpp.getName().equals(property.getName())) {
                return (T) property;
            }
        }

        T defaultMapping = properties.get("*");
        if(defaultMapping != null) {
            try {
                return (T)((Property)defaultMapping).clone();
            } catch (CloneNotSupportedException e) {
                return BeanUtils.instantiate(getPropertyMappedFormType());
            }
        }
        else {
            return BeanUtils.instantiate(getPropertyMappedFormType());
        }
    }
}
