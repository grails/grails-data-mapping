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
package org.grails.datastore.gorm.mongo.config

import org.grails.datastore.mapping.mongo.MongoDatastore
import org.springframework.context.support.GenericApplicationContext
import org.grails.datastore.mapping.mongo.config.MongoMappingContext
import org.grails.datastore.mapping.transactions.DatastoreTransactionManager
import org.grails.datastore.gorm.mongo.MongoGormEnhancer
import org.grails.datastore.gorm.events.AutoTimestampEventListener
import org.grails.datastore.gorm.events.DomainEventListener
import org.codehaus.groovy.grails.validation.GrailsDomainClassValidator
import org.springframework.context.ApplicationContext
import org.springframework.context.ConfigurableApplicationContext
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler

/**
 * Support class for easing configuration of MongoDB
 */
class MongoDatastoreConfigurer {

    /**
     * Configures MongoDB for usage via GORM
     *
     * @param databaseName The database name
     * @param classes The classes
     * @param configuration The configuration
     * @return The MongoDatastore instance
     */

    static MongoDatastore configure(String databaseName, Class... classes, Map configuration = Collections.emptyMap() ) {
        ExpandoMetaClass.enableGlobally()

        def ctx = new GenericApplicationContext()
        ctx.refresh()
        return configure(ctx, databaseName, classes, configuration)
    }

    static MongoDatastore configure(ConfigurableApplicationContext ctx, String databaseName, Class... classes, Map configuration) {
        final context = new MongoMappingContext(databaseName)
        def grailsApplication = new DefaultGrailsApplication(classes, Thread.currentThread().getContextClassLoader())
        grailsApplication.initialise()
        for (cls in classes) {
            def validator = new GrailsDomainClassValidator()
            validator.setGrailsApplication(grailsApplication)
            validator.setMessageSource(ctx)
            validator.setDomainClass(grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, cls.name))
            final entity = context.addPersistentEntity(cls)
            context.addEntityValidator(entity, validator)
        }
        def mongoDatastore = new MongoDatastore(context, configuration, ctx)
        mongoDatastore.afterPropertiesSet()

        def enhancer = new MongoGormEnhancer(mongoDatastore, new DatastoreTransactionManager(datastore: mongoDatastore))
        enhancer.enhance()

        mongoDatastore.applicationContext.addApplicationListener new DomainEventListener(mongoDatastore)
        mongoDatastore.applicationContext.addApplicationListener new AutoTimestampEventListener(mongoDatastore)



        return mongoDatastore
    }


}
