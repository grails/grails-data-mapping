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
package org.grails.datastore.mapping.simpledb.config;

import groovy.lang.Closure;

import org.grails.datastore.mapping.config.groovy.MappingConfigurationBuilder;
import org.grails.datastore.mapping.keyvalue.mapping.config.Family;
import org.grails.datastore.mapping.keyvalue.mapping.config.GormKeyValueMappingFactory;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.config.GormProperties;
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher;

/**
 * MappingFactory for SimpleDB.
 *
 * @author Roman Stepanenko
 * @since 0.l
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class GormSimpleDBMappingFactory extends GormKeyValueMappingFactory {

    public GormSimpleDBMappingFactory() {
        super(null);
    }

    @Override
    public Family createMappedForm(PersistentEntity entity) {
        ClassPropertyFetcher cpf = ClassPropertyFetcher.forClass(entity.getJavaClass());

        Closure value = cpf.getStaticPropertyValue(GormProperties.MAPPING, Closure.class);
        if (value == null) {
            return new SimpleDBDomainClassMappedForm(entity.getName());
        }

        Family family = new SimpleDBDomainClassMappedForm();
        MappingConfigurationBuilder builder = new MappingConfigurationBuilder(family, getPropertyMappedFormType());

        builder.evaluate(value);
        value = cpf.getStaticPropertyValue(GormProperties.CONSTRAINTS, Closure.class);
        if (value != null) {
            builder.evaluate(value);
        }
        entityToPropertyMap.put(entity, builder.getProperties());
        return family;
    }
}
