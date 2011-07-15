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
package org.springframework.datastore.mapping.simpledb.config;

import org.springframework.datastore.mapping.document.config.Attribute;
import org.springframework.datastore.mapping.document.config.Collection;
import org.springframework.datastore.mapping.model.AbstractMappingContext;
import org.springframework.datastore.mapping.model.MappingConfigurationStrategy;
import org.springframework.datastore.mapping.model.MappingFactory;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.model.config.GormMappingConfigurationStrategy;

/**
 * Models a {@link org.springframework.datastore.mapping.model.MappingContext} for SimpleDB.
 *
 * @author Roman Stepanenko based on Graeme Rocher code for MongoDb and Redis
 * @since 0.1
 */
public class SimpleDBMappingContext extends AbstractMappingContext {

    protected MappingConfigurationStrategy syntaxStrategy;
    MappingFactory<Collection, Attribute> mappingFactory;

    public SimpleDBMappingContext() {
        mappingFactory = createMappingFactory();
        syntaxStrategy = new GormMappingConfigurationStrategy(mappingFactory);
    }

    protected MappingFactory createMappingFactory() {
        return new GormSimpleDBMappingFactory();
    }

    @Override
    protected PersistentEntity createPersistentEntity(@SuppressWarnings("rawtypes") Class javaClass) {
        return new SimpleDBPersistentEntity(javaClass, this);
    }

    public MappingConfigurationStrategy getMappingSyntaxStrategy() {
        return syntaxStrategy;
    }

    public MappingFactory getMappingFactory() {
        return mappingFactory;
    }
}
