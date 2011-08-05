import org.grails.datastore.gorm.neo4j.plugin.support.*
import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.grails.datastore.gorm.neo4j.constraints.UniqueConstraint

class Neo4jGrailsPlugin {

    def license = "WTFPL"
    def organization = [ name: "Stefan Armbruster", url: "http://blog.armbruster-it.de/" ]
    def developers = [
        [ name: "Stefan Armbruster", email: "stefan@armbruster-it.de" ] ]
    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPNEO4J" ]
    def scm = [ url: "https://github.com/sarmbruster/grails-data-mapping" ]

    def version = "0.9-SNAPSHOT"
    def grailsVersion = "1.2 > *"
    //def observe = ['services']
    //def loadAfter = ['domainClass', 'hibernate', 'services', 'cloudFoundry']
    def loadAfter = ['domainClass', 'hibernate', 'services', 'cloudFoundry']
    def author = "Stefan Armbruster"
    def authorEmail = "stefan@armbruster-it.de"
    def title = "Neo4j GORM"
    def description = 'A plugin that integrates the Neo4j graph database into Grails, providing a GORM API onto it'

    def documentation = "http://grails.org/plugin/neo4j"

    def dependsOn = [:]
    // resources that are excluded from plugin packaging

    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    def doWithSpring = new Neo4jSpringConfigurer().getConfiguration()

    def doWithDynamicMethods = { ctx ->
        def datastore = ctx.neo4jDatastore
        def  transactionManager = null // ctx.neo4jTransactionManager
        def methodsConfigurer = new Neo4jMethodsConfigurer(datastore, transactionManager)    
        methodsConfigurer.hasExistingDatastore = manager.hasGrailsPlugin("hibernate")
        def foe = application?.config?.grails?.gorm?.failOnError
        methodsConfigurer.failOnError = foe instanceof Boolean ? foe : false
        
        methodsConfigurer.configure()
    }


    def doWithApplicationContext = { applicationContext ->
        ConstrainedProperty.registerNewConstraint(UniqueConstraint.UNIQUE_CONSTRAINT, UniqueConstraint.class );
    }

    def onChange = { event ->
        def onChangeHandler = new Neo4jOnChangeHandler()
        onChangeHandler.onChange(delegate, event)        
    }

}
