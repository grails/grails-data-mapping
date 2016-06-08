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

package org.grails.datastore.gorm.neo4j.bean.factory

import groovy.transform.CompileStatic
import org.grails.datastore.gorm.events.AutoTimestampEventListener
import org.grails.datastore.gorm.events.ConfigurableApplicationContextEventPublisher
import org.grails.datastore.gorm.events.DomainEventListener
import org.grails.datastore.gorm.neo4j.Neo4jDatastore
import org.springframework.beans.factory.FactoryBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.grails.datastore.mapping.model.MappingContext
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.PropertyResolver

/**
 * Factory bean for constructing a {@link Neo4jDatastore} instance.
 *
 * @author Stefan Armbruster
 * @deprecated No longer necessary, use the constructors of {@link Neo4jDatastore} instead
 */
@CompileStatic
@Deprecated
class Neo4jDatastoreFactoryBean implements FactoryBean<Neo4jDatastore>, ApplicationContextAware {

    MappingContext mappingContext
    PropertyResolver configuration
    ApplicationContext applicationContext

    Neo4jDatastore getObject() {
        return new Neo4jDatastore(mappingContext, configuration, new ConfigurableApplicationContextEventPublisher((ConfigurableApplicationContext)applicationContext) )
    }

    Class<?> getObjectType() { Neo4jDatastore }

    boolean isSingleton() { true }
}
