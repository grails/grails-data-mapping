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

package org.grails.datastore.gorm.simpledb.bean.factory

import org.grails.datastore.gorm.events.AutoTimestampEventListener
import org.grails.datastore.gorm.events.DomainEventListener
import org.springframework.beans.factory.FactoryBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.simpledb.SimpleDBDatastore
import org.grails.datastore.mapping.cache.TPCacheAdapterRepository
import org.grails.datastore.mapping.simpledb.engine.SimpleDBNativeItem

/**
 * Factory bean for constructing a {@link org.grails.datastore.mapping.simpledb.SimpleDBDatastore} instance.
 *
 * @author Roman Stepanenko based on Graeme Rocher code for MongoDb and Redis
 * @since 0.1
 */

class SimpleDBDatastoreFactoryBean implements FactoryBean<SimpleDBDatastore>, ApplicationContextAware {

    MappingContext mappingContext
    Map<String,String> config = [:]
    ApplicationContext applicationContext
    TPCacheAdapterRepository<SimpleDBNativeItem> cacheAdapterRepository

    SimpleDBDatastore getObject() {

        SimpleDBDatastore datastore = new SimpleDBDatastore(mappingContext, config, applicationContext, cacheAdapterRepository)

        applicationContext.addApplicationListener new DomainEventListener(datastore)
        applicationContext.addApplicationListener new AutoTimestampEventListener(datastore)

        datastore.afterPropertiesSet()
        datastore
    }

    Class<?> getObjectType() { SimpleDBDatastore }

    boolean isSingleton() { true }
}