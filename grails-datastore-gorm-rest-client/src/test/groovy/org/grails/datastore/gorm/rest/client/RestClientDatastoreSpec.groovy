package org.grails.datastore.gorm.rest.client

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationHolder
import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationInitializer

import org.grails.datastore.gorm.events.AutoTimestampEventListener
import org.grails.datastore.gorm.events.DomainEventListener
import org.grails.datastore.gorm.plugin.support.DynamicMethodsConfigurer
import org.grails.datastore.gorm.rest.client.plugin.support.RestClientMethodsConfigurer

import org.grails.datastore.mapping.rest.client.RestClientDatastore
import org.grails.datastore.mapping.rest.client.RestClientSession
import org.grails.datastore.mapping.transactions.DatastoreTransactionManager
import org.springframework.context.support.GenericApplicationContext
import spock.lang.Shared
import spock.lang.Specification

/**
 * @author Graeme Rocher
 */
abstract class RestClientDatastoreSpec extends Specification{

    @Shared RestClientDatastore datastore
    RestClientSession session


    void setupSpec() {
        final application = new DefaultGrailsApplication(getDomainClasses() as Class[], new GroovyClassLoader())
        application.initialise()
        new ConvertersConfigurationInitializer().initialize(application)

        def applicationContext = new GenericApplicationContext()
        applicationContext.refresh()
        datastore = new RestClientDatastore()
        datastore.applicationContext = applicationContext
        for (cls in getDomainClasses()) {
            datastore.mappingContext.addPersistentEntity(cls)
        }

        def txMgr = new DatastoreTransactionManager(datastore: datastore)
        DynamicMethodsConfigurer methodsConfigurer = new RestClientMethodsConfigurer(datastore, txMgr)
        methodsConfigurer.configure()


        datastore.applicationContext.addApplicationListener new DomainEventListener(datastore)
        datastore.applicationContext.addApplicationListener new AutoTimestampEventListener(datastore)


    }

    void cleanupSpec() {
        ConvertersConfigurationHolder.clear()
    }

    void setup() {
        session  = (RestClientSession)datastore.connect()
    }

    abstract List<Class> getDomainClasses()
}
