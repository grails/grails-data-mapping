package grails.plugins.mongodb

import com.mongodb.BasicDBList
import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import grails.core.GrailsClass
import grails.mongodb.bootstrap.MongoDbDataStoreSpringInitializer
import grails.plugins.Plugin
import grails.util.Metadata
import groovy.transform.CompileStatic
import org.grails.core.artefact.DomainClassArtefactHandler
import org.grails.datastore.mapping.mongo.MongoDatastore
import org.grails.web.json.JSONWriter
import grails.converters.JSON
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.transaction.PlatformTransactionManager

class MongodbGrailsPlugin extends Plugin {
    def license = "Apache 2.0 License"
    def organization = [name: "Grails", url: "http://grails.org/"]
    def developers = [
        [name: "Graeme Rocher", email: "graeme@grails.org"]]
    def issueManagement = [system: "Github", url: "https://github.com/grails/grails-data-mapping"]
    def scm = [url: "https://github.com/grails/grails-data-mapping"]

    def grailsVersion = "2.5.0 > *"
    def observe = ['services', 'domainClass']
    def loadAfter = ['domainClass', 'hibernate', 'hibernate4', 'services']
    def author = "Graeme Rocher"
    def authorEmail = "graeme@grails.org"
    def title = "MongoDB GORM"
    def description = 'A plugin that integrates the MongoDB document datastore into Grails, providing a GORM API onto it'

    def documentation = "http://grails.github.io/grails-data-mapping/latest/mongodb/"

    @Override
    @CompileStatic
    Closure doWithSpring() {
        def initializer = new MongoDbDataStoreSpringInitializer(config, grailsApplication.getArtefacts(DomainClassArtefactHandler.TYPE).collect() { GrailsClass cls -> cls.clazz })
        initializer.registerApplicationIfNotPresent = false

        def applicationName = Metadata.getCurrent().getApplicationName()
        if(!applicationName.contains('@')) {
            initializer.databaseName = applicationName
        }
        initializer.setSecondaryDatastore( manager.hasGrailsPlugin("hibernate")  )

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
    void onChange(Map<String, Object> event) {

        def ctx = applicationContext
        event.application = grailsApplication
        event.ctx = applicationContext

        def mongoDatastore = ctx.getBean(MongoDatastore)
        def mongoTransactionManager = ctx.getBean('mongoTransactionManager', PlatformTransactionManager)
    }
}
