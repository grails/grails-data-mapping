/*
 * Copyright (c) 2010 by J. Brisbin <jon@jbrisbin.com>
 * Portions (c) 2010 by NPC International, Inc. or the
 * original author(s).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.grails.datastore.gorm.GormInstanceApi
import org.grails.datastore.gorm.riak.RiakDatastoreFactoryBean
import org.grails.datastore.gorm.riak.RiakGormEnhancer
import org.grails.datastore.gorm.riak.RiakGormStaticApi
import org.grails.datastore.gorm.riak.RiakMappingContextFactoryBean
import org.grails.datastore.gorm.support.DatastorePersistenceContextInterceptor
import org.grails.datastore.gorm.utils.InstanceProxy
import org.springframework.data.keyvalue.riak.core.AsyncRiakTemplate
import org.springframework.data.keyvalue.riak.core.RiakTemplate
import org.springframework.data.keyvalue.riak.groovy.RiakBuilder
import org.springframework.datastore.mapping.core.Datastore
import org.springframework.datastore.mapping.reflect.ClassPropertyFetcher
import org.springframework.datastore.mapping.transactions.DatastoreTransactionManager
import org.springframework.datastore.mapping.web.support.OpenSessionInViewInterceptor
import org.springframework.transaction.PlatformTransactionManager

class RiakGrailsPlugin {
  // the plugin version
  def version = "1.0.0.M3"
  // the version or versions of Grails the plugin is designed for
  def grailsVersion = "1.3.5 > *"
  // the other plugins this plugin depends on
  def dependsOn = [:]
  // resources that are excluded from plugin packaging
  def pluginExcludes = [
      "grails-app/views/error.gsp"
  ]

  def author = "Jon Brisbin"
  def authorEmail = "jbrisbin@vmware.com"
  def title = "Riak GORM"
  def description = '''\\
A plugin that integrates the Riak document/data store into Grails.
'''

  def observe = ['controllers', 'services']

  // URL to the plugin's documentation
  def documentation = "http://grails.org/plugin/riak"

  def doWithSpring = {
    def riakConfig = application.config?.grails?.riak

    riakTransactionManager(DatastoreTransactionManager) {
      datastore = ref("springDatastore")
    }

    datastoreMappingContext(RiakMappingContextFactoryBean) {
      grailsApplication = ref('grailsApplication')
      //pluginManager = ref('pluginManager')
    }

    springDatastore(RiakDatastoreFactoryBean) {
      config = riakConfig
      mappingContext = ref("datastoreMappingContext")
      //pluginManager = ref('pluginManager')
    }

    riakTemplate(RiakTemplate) { bean ->
      def riakDefaultUri = riakConfig?.remove("defaultUri")
      if (riakDefaultUri) {
        defaultUri = riakDefaultUri
      }
      def riakMapRedUri = riakConfig?.remove("mapReduceUri")
      if (riakMapRedUri) {
        mapReduceUri = riakMapRedUri
      }
      def riakUseCache = riakConfig?.remove("useCache")
      if (null != riakUseCache) {
        useCache = riakUseCache
      }
    }

    asyncRiakTemplate(AsyncRiakTemplate) { bean ->
      def riakDefaultUri = riakConfig?.remove("defaultUri")
      if (riakDefaultUri) {
        defaultUri = riakDefaultUri
      }
      def riakMapRedUri = riakConfig?.remove("mapReduceUri")
      if (riakMapRedUri) {
        mapReduceUri = riakMapRedUri
      }
      def riakUseCache = riakConfig?.remove("useCache")
      if (riakUseCache) {
        useCache = riakUseCache
      }
    }

    riak(RiakBuilder, asyncRiakTemplate) { bean ->
      bean.scope = "prototype"
    }

    datastorePersistenceInterceptor(DatastorePersistenceContextInterceptor, ref("springDatastore"))
    if (manager?.hasGrailsPlugin("controllers")) {
      riakOpenSessionInViewInterceptor(OpenSessionInViewInterceptor) {
        datastore = ref("springDatastore")
      }
      if (getSpringConfig().containsBean("controllerHandlerMappings")) {
        controllerHandlerMappings.interceptors << riakOpenSessionInViewInterceptor
      }
      if (getSpringConfig().containsBean("annotationHandlerMapping")) {
        if (annotationHandlerMapping.interceptors) {
          annotationHandlerMapping.interceptors << riakOpenSessionInViewInterceptor
        } else {
          annotationHandlerMapping.interceptors = [riakOpenSessionInViewInterceptor]
        }
      }
    }

  }

  def doWithDynamicMethods = { ctx ->

    Datastore store = ctx.getBean("springDatastore")
    PlatformTransactionManager transactionManager = ctx.getBean("riakTransactionManager")
    def enhancer = transactionManager ? new RiakGormEnhancer(store, transactionManager) : new RiakGormEnhancer(store)

    def isHibernateInstalled = manager.hasGrailsPlugin("hibernate")
    for (entity in store.mappingContext.persistentEntities) {
      if (isHibernateInstalled) {
        def cls = entity.javaClass
        def cpf = ClassPropertyFetcher.forClass(cls)
        def mappedWith = cpf.getStaticPropertyValue(GrailsDomainClassProperty.MAPPING_STRATEGY, String)
        if (mappedWith == 'riak') {
          enhancer.enhance(entity)
        } else {
          def staticApi = new RiakGormStaticApi(cls, store)
          def instanceApi = new GormInstanceApi(cls, store)
          cls.metaClass.static.getRiak = {-> staticApi }
          cls.metaClass.getRiak = {-> new InstanceProxy(instance: delegate, target: instanceApi) }
        }
      } else {
        enhancer.enhance(entity)
      }
    }

    configureRiak(application, ctx)
  }

  def onChange = { event ->
    configureRiak(event.application, event.ctx)
  }

  def configureRiak(app, ctx) {
    app.controllerClasses.each {
      it.metaClass.riak = { Closure cl -> doWithRiak(ctx, cl) }
    }
    app.serviceClasses.each {
      it.metaClass.riak = { Closure cl -> doWithRiak(ctx, cl) }
    }
  }

  def doWithRiak(ctx, cl) {
    def riak = new RiakBuilder(ctx.asyncRiakTemplate)
    cl.delegate = riak
    cl.resolveStrategy = Closure.DELEGATE_FIRST
    cl.call()
    return riak
  }

}
