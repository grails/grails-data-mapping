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
package org.grails.datastore.mapping.jpa.config;


import org.grails.datastore.mapping.model.AbstractMappingContext;
import org.grails.datastore.mapping.model.MappingConfigurationStrategy;
import org.grails.datastore.mapping.model.MappingFactory;
import org.grails.datastore.mapping.model.PersistentEntity;

/**
 * A MappingContext for JPA compatible entities.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings("rawtypes")
public class JpaMappingContext extends AbstractMappingContext{

    private MappingFactory<org.grails.datastore.mapping.jpa.config.Table, org.grails.datastore.mapping.jpa.config.Column> mappingFactory = new JpaMappingFactory();
    private MappingConfigurationStrategy jpaMappingSyntaxStrategy = new JpaMappingConfigurationStrategy(mappingFactory);

    public MappingConfigurationStrategy getMappingSyntaxStrategy() {
        return jpaMappingSyntaxStrategy ;
    }

    @Override
    public MappingFactory getMappingFactory() {
        return mappingFactory;
    }

    @Override
    protected PersistentEntity createPersistentEntity(Class javaClass) {
        return new JpaPersistentEntity(javaClass, this);
    }
}
