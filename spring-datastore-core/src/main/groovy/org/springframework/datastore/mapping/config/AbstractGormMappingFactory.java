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

package org.springframework.datastore.mapping.config;

import groovy.lang.Closure;
import org.springframework.beans.BeanUtils;
import org.springframework.datastore.mapping.config.groovy.MappingConfigurationBuilder;
import org.springframework.datastore.mapping.model.MappingFactory;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.model.PersistentProperty;
import org.springframework.datastore.mapping.model.config.GormProperties;
import org.springframework.datastore.mapping.reflect.ClassPropertyFetcher;

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract GORM implementation that uses the GORM MappingConfigurationBuilder to configure entity mappings
 * 
 * @author Graeme Rocher
 *
 */
public abstract class AbstractGormMappingFactory<R, T> extends MappingFactory<R, T> {
	
    protected Map<PersistentEntity, Map<String, T>> entityToPropertyMap = new HashMap<PersistentEntity, Map<String, T>>();

	@SuppressWarnings("unchecked")
	@Override
	public R createMappedForm(PersistentEntity entity) {
        ClassPropertyFetcher cpf = ClassPropertyFetcher.forClass(entity.getJavaClass());
        R family = BeanUtils.instantiate(getEntityMappedFormType());
        MappingConfigurationBuilder builder = new MappingConfigurationBuilder(family, getPropertyMappedFormType());

        Closure value = cpf.getStaticPropertyValue(GormProperties.MAPPING, Closure.class);
        if(value != null) {
            builder.evaluate(value);
        }
        value = cpf.getStaticPropertyValue(GormProperties.CONTRAINTS, Closure.class);
        if(value != null) {
            builder.evaluate(value);
        }
        entityToPropertyMap.put(entity, builder.getProperties());
        return family;
    }

	protected abstract Class<T> getPropertyMappedFormType();

	protected abstract Class<R> getEntityMappedFormType();

	@Override
	public T createMappedForm(@SuppressWarnings("rawtypes") PersistentProperty mpp) {
        Map<String, T> properties = entityToPropertyMap.get(mpp.getOwner());
        if(properties != null && properties.containsKey(mpp.getName())) {
            return properties.get(mpp.getName());
        }
        return BeanUtils.instantiate(getPropertyMappedFormType());
	}


}
