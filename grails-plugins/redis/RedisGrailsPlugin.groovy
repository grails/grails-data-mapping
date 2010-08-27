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

import org.grails.plugins.redis.RedisMappingContextFactoryBean
import org.grails.plugins.redis.RedisDatastoreFactoryBean
import org.grails.datastore.gorm.support.DatastorePersistenceContextInterceptor
import org.springframework.datastore.web.support.OpenSessionInViewInterceptor
import javax.persistence.FlushModeType

class RedisGrailsPlugin {
    // the plugin version
    def version = "0.1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.3.4 > *"
    // the other plugins this plugin depends on
    def dependsOn = [:]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    def author = "Graeme Rocher"
    def authorEmail = "graeme.rocher@springsource.com"
    def title = "Redis GORM"
    def description = '''\\
A plugin that integrates the Redis key/value datastore into Grails, providing
a GORM-like API onto it
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/redis"

    def doWithSpring = {
        def redisConfig = application.config?.grails?.redis
        datastoreMappingContext(RedisMappingContextFactoryBean) {
          grailsApplication = ref('grailsApplication')
          pluginManager = ref('pluginManager')
        }
        springDatastore(RedisDatastoreFactoryBean) {
          config = redisConfig
          mappingContext = ref("datastoreMappingContext")
        }
        datastorePersistenceInterceptor(DatastorePersistenceContextInterceptor, ref("springDatastore"))

        if (manager?.hasGrailsPlugin("controllers")) {
            datastoreOpenSessionInViewInterceptor(OpenSessionInViewInterceptor) {
                datastore = ref("springDatastore")
            }
            if (getSpringConfig().containsBean("controllerHandlerMappings")) {
                controllerHandlerMappings.interceptors << datastoreOpenSessionInViewInterceptor
            }
            if (getSpringConfig().containsBean("annotationHandlerMapping")) {
                if (annotationHandlerMapping.interceptors) {
                    annotationHandlerMapping.interceptors << datastoreOpenSessionInViewInterceptor
                }
                else {
                    annotationHandlerMapping.interceptors = [datastoreOpenSessionInViewInterceptor]
                }
            }
        }
    }

    def doWithApplicationContext = { ctx ->
      ctx.getBean("springDatastore") // initialize it
    }
}
