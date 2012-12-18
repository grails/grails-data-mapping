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

package org.grails.datastore.mapping.document.config;

import groovy.lang.Closure;
import org.grails.datastore.mapping.model.AbstractMappingContext;
import org.grails.datastore.mapping.model.MappingConfigurationStrategy;
import org.grails.datastore.mapping.model.MappingFactory;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.config.GormMappingConfigurationStrategy;
import org.springframework.util.Assert;

/**
 * Models a {@link org.grails.datastore.mapping.model.MappingContext} for a Document store.
 *
 * @author Graeme Rocher
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class DocumentMappingContext extends AbstractMappingContext {
    String defaultDatabaseName;
    MappingFactory<Collection, Attribute> mappingFactory;

    private MappingConfigurationStrategy syntaxStrategy;
    private Closure defaultMapping;

    public DocumentMappingContext(String defaultDatabaseName) {
        Assert.notNull(defaultDatabaseName, "Argument [defaultDatabaseName] cannot be null");
        this.defaultDatabaseName = defaultDatabaseName;
        mappingFactory = createDocumentMappingFactory(null);
        syntaxStrategy = new GormMappingConfigurationStrategy(mappingFactory);
    }
    public DocumentMappingContext(String defaultDatabaseName, Closure defaultMapping) {
        Assert.notNull(defaultDatabaseName, "Argument [defaultDatabaseName] cannot be null");
        this.defaultDatabaseName = defaultDatabaseName;
        mappingFactory = createDocumentMappingFactory(defaultMapping);
        this.defaultMapping = defaultMapping;
        syntaxStrategy = new GormMappingConfigurationStrategy(mappingFactory);
    }

    public Closure getDefaultMapping() {
        return defaultMapping;
    }

    protected MappingFactory createDocumentMappingFactory(Closure defaultMapping) {
        return new GormDocumentMappingFactory();
    }

    public String getDefaultDatabaseName() {
        return defaultDatabaseName;
    }

    public MappingConfigurationStrategy getMappingSyntaxStrategy() {
        return syntaxStrategy;
    }

    @Override
    public MappingFactory<Collection, Attribute> getMappingFactory() {
        return mappingFactory;
    }

    @Override
    protected PersistentEntity createPersistentEntity(Class javaClass) {
        return new DocumentPersistentEntity(javaClass, this);
    }
}
