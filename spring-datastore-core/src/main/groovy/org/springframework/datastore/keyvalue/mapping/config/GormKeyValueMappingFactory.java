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
package org.springframework.datastore.keyvalue.mapping.config;

import groovy.lang.Closure;
import org.springframework.datastore.config.groovy.MappingConfigurationBuilder;
import org.springframework.datastore.keyvalue.mapping.Family;
import org.springframework.datastore.keyvalue.mapping.KeyValue;
import org.springframework.datastore.keyvalue.mapping.KeyValueMappingFactory;
import org.springframework.datastore.mapping.PersistentEntity;
import org.springframework.datastore.mapping.PersistentProperty;
import org.springframework.datastore.mapping.config.GormProperties;
import org.springframework.datastore.reflect.ClassPropertyFetcher;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Graeme Rocher
 * @since 1.0
 */
public class GormKeyValueMappingFactory extends KeyValueMappingFactory {

    private Map<PersistentEntity, Map> entityToPropertyMap = new HashMap<PersistentEntity, Map>();

    public GormKeyValueMappingFactory(String keyspace) {
        super(keyspace);
    }

    @Override
    public Family createMappedForm(PersistentEntity entity) {
        ClassPropertyFetcher cpf = ClassPropertyFetcher.forClass(entity.getJavaClass());
        final Closure value = cpf.getStaticPropertyValue(GormProperties.MAPPING, Closure.class);
        if(value != null) {
            Family family = new Family();
            MappingConfigurationBuilder builder = new MappingConfigurationBuilder(family, KeyValue.class);
            builder.evaluate(value);
            entityToPropertyMap.put(entity, builder.getProperties());
            return family;
        }
        else {
            return super.createMappedForm(entity);
        }
    }

    @Override
    public KeyValue createMappedForm(PersistentProperty mpp) {
        Map properties = entityToPropertyMap.get(mpp.getOwner());
        if(properties != null && properties.containsKey(mpp.getName())) {
            return (KeyValue) properties.get(mpp.getName());
        }
        return super.createMappedForm(mpp);
    }
}
