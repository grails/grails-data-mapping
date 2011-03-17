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
package org.springframework.datastore.mapping.gemfire.config;

import groovy.lang.Closure;

import org.springframework.datastore.mapping.config.groovy.MappingConfigurationBuilder;
import org.springframework.datastore.mapping.keyvalue.mapping.config.Family;
import org.springframework.datastore.mapping.keyvalue.mapping.config.GormKeyValueMappingFactory;
import org.springframework.datastore.mapping.keyvalue.mapping.config.KeyValue;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.model.config.GormProperties;
import org.springframework.datastore.mapping.reflect.ClassPropertyFetcher;

import com.gemstone.gemfire.cache.AttributesFactory;
import com.gemstone.gemfire.cache.DataPolicy;
import com.gemstone.gemfire.cache.RegionAttributes;

/**
 *
 * Allows GORM-style configuration of how an entity maps to a
 * Gemfire region
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class GormGemfireMappingFactory extends GormKeyValueMappingFactory {

    private DataPolicy defaultDataPolicy = DataPolicy.PARTITION;

    public GormGemfireMappingFactory() {
        super("Gemfire");
    }

    public void setDefaultDataPolicy(DataPolicy defaultDataPolicy) {
        this.defaultDataPolicy = defaultDataPolicy;
    }

    @Override
    public Family createMappedForm(PersistentEntity entity) {
        ClassPropertyFetcher cpf = ClassPropertyFetcher.forClass(entity.getJavaClass());
        final Closure value = cpf.getStaticPropertyValue(GormProperties.MAPPING, Closure.class);
        if (value != null) {
            final Region family = new Region();
            AttributesFactory factory = new AttributesFactory() {
                @SuppressWarnings("unused")
                public void setRegion(String name) {
                    family.setRegion(name);
                }
            };
            factory.setDataPolicy(defaultDataPolicy);
            MappingConfigurationBuilder builder = new MappingConfigurationBuilder(factory, KeyValue.class);
            builder.evaluate(value);
            entityToPropertyMap.put(entity, builder.getProperties());
            final RegionAttributes regionAttributes = factory.create();
            family.setRegionAttributes(regionAttributes);
            family.setCacheListeners(regionAttributes.getCacheListeners());
            family.setDataPolicy(regionAttributes.getDataPolicy());
            family.setCacheLoader(regionAttributes.getCacheLoader());
            family.setCacheWriter(regionAttributes.getCacheWriter());
            return family;
        }
        return new Region();
    }
}
