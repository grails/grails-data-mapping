
import org.grails.datastore.gorm.mongo.plugin.support.MongoMethodsConfigurer
import org.grails.datastore.gorm.mongo.plugin.support.MongoOnChangeHandler
import org.grails.datastore.gorm.mongo.plugin.support.MongoSpringConfigurer

class MongodbGrailsPlugin {
    def license = "Apache 2.0 License"
    def organization = [name: "SpringSource", url: "http://www.springsource.org/"]
    def developers = [
        [name: "Jeff Beck", email: "beckje01@gmail.com"]]
    def issueManagement = [system: "todo", url: "todo"]
    def scm = [url: "https://github.com/grails/grails-data-mapping"]

    def version = "0.1-SNAPSHOT"
    def grailsVersion = "2.1.4 > *"
    def observe = ['services', 'domainClass']
    def loadAfter = ['domainClass', 'hibernate', 'services', 'cloudFoundry']
    def author = "Jeff Beck"
    def authorEmail = "beckje01@gmail.com"
    def title = "Cassandra GORM"
    def description = ''

    def documentation = ""

//    def doWithSpring = new MongoSpringConfigurer().getConfiguration()

    def doWithDynamicMethods = { ctx ->
        def datastore = ctx.mongoDatastore
        def transactionManager = ctx.mongoTransactionManager
        def methodsConfigurer = new MongoMethodsConfigurer(datastore, transactionManager)    
        methodsConfigurer.hasExistingDatastore = manager.hasGrailsPlugin("hibernate")        
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
