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
package org.grails.datastore.gorm.redis.bean.factory

import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.redis.RedisDatastore
import org.springframework.beans.factory.FactoryBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

/**
 * Creates the RedisDatastore bean
 */
class RedisDatastoreFactoryBean implements FactoryBean<RedisDatastore>, ApplicationContextAware {

    Map<String, String> config
    MappingContext mappingContext
    GrailsPluginManager pluginManager
    ApplicationContext applicationContext

    RedisDatastore getObject() {
        new RedisDatastore(mappingContext, config, applicationContext)
    }

    Class<?> getObjectType() { RedisDatastore }

    boolean isSingleton() { true }
}
