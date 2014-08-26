
import org.grails.datastore.gorm.mongo.plugin.support.MongoMethodsConfigurer
import org.grails.datastore.gorm.mongo.plugin.support.MongoOnChangeHandler
import org.grails.datastore.gorm.mongo.plugin.support.MongoSpringConfigurer
import grails.converters.*
import org.codehaus.groovy.grails.web.json.JSONWriter
import com.mongodb.*


class MongodbGrailsPlugin {
    def license = "Apache 2.0 License"
    def organization = [name: "SpringSource", url: "http://www.springsource.org/"]
    def developers = [
        [name: "Graeme Rocher", email: "grocher@vmware.com"]]
    def issueManagement = [system: "JIRA", url: "http://jira.grails.org/browse/GPMONGODB"]
    def scm = [url: "https://github.com/grails/grails-data-mapping"]

    def version = "3.0.2"
    def grailsVersion = "2.3.2 > *"
    def observe = ['services', 'domainClass']
    def loadAfter = ['domainClass', 'hibernate', 'hibernate4', 'services', 'cloudFoundry']
    def author = "Graeme Rocher"
    def authorEmail = "graeme.rocher@springsource.com"
    def title = "MongoDB GORM"
    def description = 'A plugin that integrates the Mongo document datastore into Grails, providing a GORM API onto it'

    def documentation = "http://grails.github.io/grails-data-mapping/mongodb"

    def doWithSpring = new MongoSpringConfigurer().getConfiguration()

    def doWithApplicationContext = { 
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

    def doWithDynamicMethods = { ctx ->
        def datastore = ctx.mongoDatastore
        def transactionManager = ctx.mongoTransactionManager
        def methodsConfigurer = new MongoMethodsConfigurer(datastore, transactionManager)    
        methodsConfigurer.hasExistingDatastore = manager.hasGrailsPlugin("hibernate") || manager.hasGrailsPlugin("hibernate4")
        def foe = application?.config?.grails?.gorm?.failOnError
        methodsConfigurer.failOnError = foe instanceof Boolean ? foe : false
        methodsConfigurer.configure()
    }

    def onChange = { event ->
        if(event.ctx) {
            new MongoOnChangeHandler(event.ctx.mongoDatastore, event.ctx.mongoTransactionManager).onChange(delegate, event)            
        }
    }   
}
