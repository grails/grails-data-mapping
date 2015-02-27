package grails.plugins.mongodb

import com.mongodb.BasicDBList
import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import grails.core.GrailsClass
import grails.mongodb.bootstrap.MongoDbDataStoreSpringInitializer
import grails.plugins.Plugin
import groovy.transform.CompileStatic
import org.grails.core.artefact.DomainClassArtefactHandler
import org.grails.datastore.gorm.mongo.plugin.support.MongoMethodsConfigurer
import org.grails.datastore.gorm.mongo.plugin.support.MongoOnChangeHandler
import org.grails.datastore.mapping.mongo.MongoDatastore
import org.grails.web.json.JSONWriter
import grails.converters.JSON
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.transaction.PlatformTransactionManager

class MongodbGrailsPlugin extends Plugin {
    def license = "Apache 2.0 License"
    def organization = [name: "SpringSource", url: "http://www.springsource.org/"]
    def developers = [
        [name: "Graeme Rocher", email: "grocher@vmware.com"]]
    def issueManagement = [system: "JIRA", url: "http://jira.grails.org/browse/GPMONGODB"]
    def scm = [url: "https://github.com/grails/grails-data-mapping"]

    def grailsVersion = "3.0.0 > *"
    def observe = ['services', 'domainClass']
    def loadAfter = ['domainClass', 'hibernate', 'hibernate4', 'services', 'cloudFoundry']
    def author = "Graeme Rocher"
    def authorEmail = "graeme.rocher@springsource.com"
    def title = "MongoDB GORM"
    def description = 'A plugin that integrates the Mongo document datastore into Grails, providing a GORM API onto it'

    def documentation = "http://grails.github.io/grails-data-mapping/mongodb"

    @Override
    Closure doWithSpring() {
        def initializer = new MongoDbDataStoreSpringInitializer(config, grailsApplication.getArtefacts(DomainClassArtefactHandler.TYPE).collect() { GrailsClass cls -> cls.clazz })
        initializer.registerApplicationIfNotPresent = false
        return initializer.getBeanDefinitions((BeanDefinitionRegistry)applicationContext)
    }

    @Override
    void doWithApplicationContext() {
        JSON.registerObjectMarshaller DBObject, { dbo, json ->
            JSONWriter writer = json.getWriter();

            writer.object()
            dbo.each {  k, v ->
                writer.key(k)
                json.convertAnother(v)
            }
            writer.endObject()

            null
        }
        JSON.registerObjectMarshaller(BasicDBList, 999) { dbo, json ->
            JSONWriter writer = json.getWriter();

            writer.array();
            dbo.each { val -> json.convertAnother(val) }
            writer.endArray();

            null
        }

        JSON.registerObjectMarshaller(BasicDBObject, 999) { dbo, json ->
            JSONWriter writer = json.getWriter();

            writer.object()
            dbo.each {  k, v ->
                writer.key(k)
                json.convertAnother(v)
            }
            writer.endObject()

            null
        }
    }

    @Override
    @CompileStatic
    void doWithDynamicMethods() {
        def ctx = applicationContext
        def datastore = ctx.getBean(MongoDatastore)
        def transactionManager = ctx.getBean('mongoTransactionManager', PlatformTransactionManager)
        def methodsConfigurer = new MongoMethodsConfigurer(datastore, transactionManager)
        methodsConfigurer.hasExistingDatastore = manager.hasGrailsPlugin("hibernate") || manager.hasGrailsPlugin("hibernate4")
        methodsConfigurer.failOnError = config.getProperty('grails.gorm.failOnError', Boolean, false)
        methodsConfigurer.configure()
    }

    @Override
    @CompileStatic
    void onChange(Map<String, Object> event) {

        def ctx = applicationContext
        event.application = grailsApplication
        event.ctx = applicationContext

        def mongoDatastore = ctx.getBean(MongoDatastore)
        def mongoTransactionManager = ctx.getBean('mongoTransactionManager', PlatformTransactionManager)
        new MongoOnChangeHandler(mongoDatastore, mongoTransactionManager).onChange(this, event)
    }
}
