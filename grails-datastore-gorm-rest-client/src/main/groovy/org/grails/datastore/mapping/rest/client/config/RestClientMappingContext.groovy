/* Copyright (C) 2013 original authors
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
package org.grails.datastore.mapping.rest.client.config

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.model.AbstractMappingContext
import org.grails.datastore.mapping.model.MappingConfigurationStrategy
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.config.GormMappingConfigurationStrategy

/**
 * Mapping context for the GORM REST client
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
class RestClientMappingContext extends AbstractMappingContext {

    private MappingConfigurationStrategy syntaxStrategy;
    RestClientMappingFactory mappingFactory

    RestClientMappingContext(Closure defaultMapping = null) {
        this.mappingFactory = new RestClientMappingFactory()
        if(defaultMapping) {
            mappingFactory.setDefaultMapping(defaultMapping)
        }
        this.syntaxStrategy = new GormMappingConfigurationStrategy(mappingFactory)

    }

    @Override
    MappingConfigurationStrategy getMappingSyntaxStrategy() {
        return this.syntaxStrategy
    }

    @Override
    protected PersistentEntity createPersistentEntity(Class javaClass) {
        return new RestClientEntity(javaClass, this)
    }

    @Override
    protected PersistentEntity createPersistentEntity(Class javaClass, boolean external) {
        return createPersistentEntity(javaClass);
    }
}
