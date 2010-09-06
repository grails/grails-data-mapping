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
import grails.datastore.Redis
import org.springframework.datastore.transactions.DatastoreTransactionManager
import org.grails.plugins.redis.InstanceProxy
import org.grails.datastore.gorm.GormInstanceApi
import org.grails.datastore.gorm.redis.RedisGormStaticApi
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.springframework.datastore.reflect.ClassPropertyFetcher
import org.grails.datastore.gorm.redis.RedisGormEnhancer
import org.springframework.context.ApplicationContext
import org.springframework.datastore.core.Datastore
import org.springframework.transaction.PlatformTransactionManager

class RedisGrailsPlugin {
    // the plugin version
    def version = "1.0.0.M1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.3.4 > *"
    // the other plugins this plugin depends on
    def dependsOn = [:]
    def loadAfter = ['domainClass']
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

        def existingTransactionManager = manager?.hasGrailsPlugin("hibernate") || getSpringConfig().containsBean("transactionManager")
        def txManagerName = existingTransactionManager ? 'datastoreTransactionManager' : 'transactionManager'

        "$txManagerName"(DatastoreTransactionManager) {
          datastore = ref("springDatastore")
        }

        datastoreMappingContext(RedisMappingContextFactoryBean) {
          grailsApplication = ref('grailsApplication')
          pluginManager = ref('pluginManager')
        }
        springDatastore(RedisDatastoreFactoryBean) {
          config = redisConfig
          mappingContext = ref("datastoreMappingContext")
          pluginManager = ref('pluginManager')
        }
        redisBean(Redis) { bean ->
          bean.scope = "request"
          datastore = ref("springDatastore")
        }
        redis(org.springframework.aop.scope.ScopedProxyFactoryBean) {
          targetBeanName = "redisBean"
          proxyTargetClass = true
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

    def doWithDynamicMethods = { ApplicationContext ctx ->
      Datastore datastore = ctx.getBean(Datastore)
      PlatformTransactionManager transactionManager = ctx.getBean(DatastoreTransactionManager)
      def enhancer = transactionManager ?
                          new RedisGormEnhancer(datastore, transactionManager) :
                          new RedisGormEnhancer(datastore)

      def isHibernateInstalled = manager.hasGrailsPlugin("hibernate")
      for(entity in datastore.mappingContext.persistentEntities) {
        if(isHibernateInstalled) {
          def cls = entity.javaClass
          def cpf = ClassPropertyFetcher.forClass(cls)
          def mappedWith = cpf.getStaticPropertyValue(GrailsDomainClassProperty.MAPPING_STRATEGY, String)
          if(mappedWith == 'redis') {
            enhancer.enhance(entity)
          }
          else {
            def staticApi = new RedisGormStaticApi(cls, datastore)
            def instanceApi = new GormInstanceApi(cls, datastore)
            cls.metaClass.static.getRedis = {-> staticApi }
            cls.metaClass.getRedis = {-> new InstanceProxy(instance:delegate, target:instanceApi) }
          }
        }
        else {
          enhancer.enhance(entity)
        }
      }

    }
}
