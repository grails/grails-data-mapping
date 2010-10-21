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
package org.springframework.datastore.mapping.keyvalue.mapping;

import org.springframework.datastore.mapping.keyvalue.mapping.config.AnnotationKeyValueMappingFactory;
import org.springframework.datastore.mapping.keyvalue.mapping.config.GormKeyValueMappingFactory;
import org.springframework.datastore.mapping.model.AbstractMappingContext;
import org.springframework.datastore.mapping.model.MappingConfigurationStrategy;
import org.springframework.datastore.mapping.model.MappingFactory;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.model.config.DefaultMappingConfigurationStrategy;
import org.springframework.datastore.mapping.model.config.GormMappingConfigurationStrategy;
import org.springframework.util.ClassUtils;

/**
 * A MappingContext used to map objects to a Key/Value store
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class KeyValueMappingContext extends AbstractMappingContext {
    protected MappingFactory<Family, KeyValue> mappingFactory;
    protected MappingConfigurationStrategy syntaxStrategy;
    public static final String GROOVY_OBJECT_CLASS = "groovy.lang.GroovyObject";

    /**
     * Constructs a context using the given keyspace
     *
     * @param keyspace The keyspace, this is typically the application name
     */
    public KeyValueMappingContext(String keyspace) {
        if(keyspace == null) throw new IllegalArgumentException("Argument [keyspace] cannot be null");
        initializeDefualtMappingFactory(keyspace);


        if(ClassUtils.isPresent(GROOVY_OBJECT_CLASS, KeyValueMappingContext.class.getClassLoader()))
            this.syntaxStrategy = new GormMappingConfigurationStrategy(mappingFactory);
        else
            this.syntaxStrategy = new DefaultMappingConfigurationStrategy(mappingFactory);



    }

    protected void initializeDefualtMappingFactory(String keyspace) {
        // TODO: Need to abstract the construction of these to support JPA syntax etc.
        if(ClassUtils.isPresent(GROOVY_OBJECT_CLASS, KeyValueMappingContext.class.getClassLoader()))
            this.mappingFactory = new GormKeyValueMappingFactory(keyspace);
        else
            this.mappingFactory = new AnnotationKeyValueMappingFactory(keyspace);
    }

    public void setMappingFactory(MappingFactory<Family, KeyValue> mappingFactory) {
        this.mappingFactory = mappingFactory;
    }

    public void setSyntaxStrategy(MappingConfigurationStrategy syntaxStrategy) {
        this.syntaxStrategy = syntaxStrategy;
    }

    public MappingConfigurationStrategy getMappingSyntaxStrategy() {
        return syntaxStrategy;
    }

    public MappingFactory<Family, KeyValue> getMappingFactory() {
        return this.mappingFactory;
    }

    @Override
    protected PersistentEntity createPersistentEntity(Class javaClass) {
        return new KeyValuePersistentEntity(javaClass, this);
    }
}
