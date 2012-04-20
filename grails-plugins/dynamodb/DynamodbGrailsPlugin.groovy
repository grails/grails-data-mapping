import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty

import org.grails.datastore.mapping.reflect.ClassPropertyFetcher
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.dynamodb.engine.DynamoDBTableResolver
import org.grails.datastore.mapping.dynamodb.engine.DynamoDBTableResolverFactory
import org.grails.datastore.mapping.dynamodb.engine.DynamoDBAssociationInfo
import org.grails.datastore.mapping.dynamodb.util.DynamoDBTemplate
import org.grails.datastore.mapping.dynamodb.util.DynamoDBConst
import org.grails.datastore.gorm.dynamodb.plugin.support.DynamoDBSpringConfigurer
import org.grails.datastore.gorm.dynamodb.plugin.support.DynamoDBMethodsConfigurer
import org.grails.datastore.gorm.dynamodb.plugin.support.DynamoDBOnChangeHandler
import org.grails.datastore.gorm.dynamodb.plugin.support.DynamoDBApplicationContextConfigurer

class DynamodbGrailsPlugin {
    def license = "Apache 2.0 License"
    def scm = [ url: "https://github.com/SpringSource/grails-data-mapping" ]
    def developers = [
        [ name: "Roman Stepanenko", email: "rs.opensource@gmail.com" ] ]
    def version = "0.1.1"
    def grailsVersion = "1.3.5 > *"
    def observe = ['services', 'domainClass']
    def loadAfter = ['domainClass', 'hibernate', 'services', 'cloudFoundry']
    def dependsOn = [:]
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    def author = "Roman Stepanenko"
    def authorEmail = "rs.opensource@gmail.com"
    def title = "DynamoDB GORM"
    def description = 'A plugin that integrates the AWS DynamoDB datastore into Grails, providing a GORM API onto it'

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/dynamodb"
    def issueManagement = [system: "JIRA", url: "http://jira.grails.org/browse/GPDYNAMODB"]

    def doWithSpring = new DynamoDBSpringConfigurer().getConfiguration()

    def doWithDynamicMethods = { ctx ->
        println 'dynamodb plugin: '+version
        def datastore = ctx.dynamodbDatastore
        def transactionManager = ctx.dynamodbTransactionManager
        def methodsConfigurer = new DynamoDBMethodsConfigurer(datastore, transactionManager)
        methodsConfigurer.hasExistingDatastore = manager.hasGrailsPlugin("hibernate")
        def foe = application?.config?.grails?.gorm?.failOnError
        methodsConfigurer.failOnError = foe instanceof Boolean ? foe : false
        
        methodsConfigurer.configure()
    }

    def doWithApplicationContext = { ctx ->
        new DynamoDBApplicationContextConfigurer().configure(ctx)
    }


    def onChange = { event ->
        if(event.ctx) {
            new DynamoDBOnChangeHandler(event.ctx.dynamodbDatastore, event.ctx.dynamodbTransactionManager).onChange(delegate, event)
        }        
    }

}
