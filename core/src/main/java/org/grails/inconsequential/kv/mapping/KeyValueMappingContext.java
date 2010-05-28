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
package org.grails.inconsequential.kv.mapping;

import org.grails.inconsequential.mapping.AbstractMappingContext;
import org.grails.inconsequential.mapping.MappingFactory;
import org.grails.inconsequential.mapping.MappingSyntaxStrategy;
import org.grails.inconsequential.mapping.PersistentEntity;
import org.grails.inconsequential.mapping.syntax.GormMappingSyntaxStrategy;

/**
 * A MappingContext used to map objects to a Key/Value store
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class KeyValueMappingContext extends AbstractMappingContext {
    protected MappingFactory<Family, KeyValue> mappingFactory;
    private MappingSyntaxStrategy syntaxStrategy;

    /**
     * Constructs a context using the given keyspace
     *
     * @param keyspace The keyspace, this is typically the application name
     */
    public KeyValueMappingContext(String keyspace) {
        this.mappingFactory = new KeyValueMappingFactory(keyspace);
        this.syntaxStrategy = new GormMappingSyntaxStrategy(mappingFactory);
    }
    public MappingSyntaxStrategy getMappingSyntaxStrategy() {
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
