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
package org.grails.datastore.gorm.redis.plugin.support

import org.grails.datastore.gorm.plugin.support.SpringConfigurer
import grails.datastore.Redis
import org.grails.datastore.gorm.redis.bean.factory.RedisMappingContextFactoryBean
import org.grails.datastore.gorm.redis.bean.factory.RedisDatastoreFactoryBean

/**
 *  Configures Spring for Redis
 */
class RedisSpringConfigurer extends SpringConfigurer{
    @Override
    String getDatastoreType() { "Redis" }

    @Override
    Closure getSpringCustomizer() {
        return {
            def redisConfig = application.config?.grails?.redis

            redisDatastoreMappingContext(RedisMappingContextFactoryBean) {
                grailsApplication = ref('grailsApplication')
                pluginManager = ref('pluginManager')
                defaultExternal = false // manager.hasGrailsPlugin("hibernate")
            }

            redisDatastore(RedisDatastoreFactoryBean) {
                config = redisConfig
                mappingContext = ref("redisDatastoreMappingContext")
                pluginManager = ref('pluginManager')
            }

            redis(Redis) { bean ->
                datastore = ref("redisDatastore")
            }
        }
    }
}
