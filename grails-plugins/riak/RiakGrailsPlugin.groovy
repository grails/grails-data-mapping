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


import org.springframework.data.keyvalue.riak.groovy.RiakBuilder
import org.grails.datastore.gorm.plugin.support.ApplicationContextConfigurer
import org.grails.datastore.gorm.riak.plugin.support.*


class RiakGrailsPlugin {
    def version = "1.0.0.M4"
    def grailsVersion = "1.3.7 > *"
    def author = "Jon Brisbin"
    def authorEmail = "jbrisbin@vmware.com"
    def title = "Riak GORM"
    def description = '''\\
A plugin that integrates the Riak document/data store into Grails.
'''

    def observe = ['controllers', 'services', 'domainClass']

    def documentation = "http://projects.spring.io/grails-data-mapping/riak/manual/index.html"

    def doWithSpring = new RiakSpringConfigurer().getConfiguration()

    def doWithDynamicMethods = { ctx ->
        def datastore = ctx.riakDatastore
        def transactionManager = ctx.riakDatastoreTransactionManager
        def methodsConfigurer = new RiakMethodsConfigurer(datastore, transactionManager)    
        methodsConfigurer.hasExistingDatastore = manager.hasGrailsPlugin("hibernate")        
        def foe = application?.config?.grails?.gorm?.failOnError
        methodsConfigurer.failOnError = foe instanceof Boolean ? foe : false        
        methodsConfigurer.configure()
        configureRiak(application, ctx)
    }

    def doWithApplicationContext = { ctx ->
        new ApplicationContextConfigurer("Riak").configure(ctx)
    }
    
    def onChange = { event ->
        if(event.ctx) {
            new RiakOnChangeHandler(event.ctx.redisDatastore, event.ctx.redisDatastoreTransactionManager).onChange(delegate, event)            
        }
        
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
