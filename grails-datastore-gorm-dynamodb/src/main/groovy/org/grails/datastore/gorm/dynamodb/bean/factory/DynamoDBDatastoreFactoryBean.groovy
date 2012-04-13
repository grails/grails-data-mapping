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

package org.grails.datastore.gorm.dynamodb.bean.factory

import org.grails.datastore.mapping.dynamodb.DynamoDBDatastore

import org.grails.datastore.mapping.dynamodb.engine.DynamoDBNativeItem
import org.grails.datastore.gorm.events.AutoTimestampEventListener
import org.grails.datastore.gorm.events.DomainEventListener
import org.grails.datastore.mapping.cache.TPCacheAdapterRepository
import org.grails.datastore.mapping.model.MappingContext
import org.springframework.beans.factory.FactoryBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

/**
 * Factory bean for constructing a {@link org.grails.datastore.mapping.dynamodb.DynamoDBDatastore} instance.
 *
 * @author Roman Stepanenko based on Graeme Rocher code for MongoDb and Redis
 * @since 0.1
 */

class DynamoDBDatastoreFactoryBean implements FactoryBean<DynamoDBDatastore>, ApplicationContextAware {

    MappingContext mappingContext
    Map<String,String> config = [:]
    ApplicationContext applicationContext
    TPCacheAdapterRepository<DynamoDBNativeItem> cacheAdapterRepository

    DynamoDBDatastore getObject() {

        DynamoDBDatastore datastore = new DynamoDBDatastore(mappingContext, config, applicationContext, cacheAdapterRepository)

        applicationContext.addApplicationListener new DomainEventListener(datastore)
        applicationContext.addApplicationListener new AutoTimestampEventListener(datastore)

        datastore.afterPropertiesSet()
        datastore
    }

    Class<?> getObjectType() { DynamoDBDatastore }

    boolean isSingleton() { true }
}