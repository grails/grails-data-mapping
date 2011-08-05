import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty

import org.grails.datastore.mapping.reflect.ClassPropertyFetcher
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.simpledb.engine.SimpleDBDomainResolver
import org.grails.datastore.mapping.simpledb.engine.SimpleDBDomainResolverFactory
import org.grails.datastore.mapping.simpledb.engine.SimpleDBAssociationInfo
import org.grails.datastore.mapping.simpledb.util.SimpleDBTemplate
import org.grails.datastore.mapping.simpledb.util.SimpleDBConst
import org.grails.datastore.gorm.simpledb.plugin.support.SimpleDBSpringConfigurer
import org.grails.datastore.gorm.simpledb.plugin.support.SimpleDBMethodsConfigurer
import org.grails.datastore.gorm.simpledb.plugin.support.SimpleDBOnChangeHandler
import org.grails.datastore.gorm.simpledb.plugin.support.SimpleDBApplicationContextConfigurer

class SimpledbGrailsPlugin {
    def license = "Apache 2.0 License"
    def scm = [ url: "https://github.com/SpringSource/grails-data-mapping" ]
    def developers = [
        [ name: "Roman Stepanenko", email: "rs.opensource@gmail.com" ] ]
    def version = "0.1"
    def grailsVersion = "1.3.5 > *"
    def observe = ['services']
    def loadAfter = ['domainClass', 'hibernate', 'services', 'cloudFoundry']
    def dependsOn = [:]
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    def author = "Roman Stepanenko"
    def authorEmail = "rs.opensource@gmail.com"
    def title = "SimpleDB GORM"
    def description = '''\\
This pluging allows to use standard GORM mapping to persist in AWS SimpleDB instead of going through JPA+SimpleJPA plugins.
This plugin will be evolved into full-fledged SimpleDB support with automatic sharding, ID generation,
configurable large String storage (in S3 or by splitting into attributes),
customizable performance tweaks according to SimpleDB best practices (dedicated columns for 'dummy null' for quick lookup by null values) etc.
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/simpledb"

    def doWithSpring = new SimpleDBSpringConfigurer().getConfiguration()

    def doWithDynamicMethods = { ctx ->

        def datastore = ctx.simpledbDatastore
        def transactionManager = ctx.simpledbTransactionManager
        def methodsConfigurer = new SimpleDBMethodsConfigurer(datastore, transactionManager)
        methodsConfigurer.hasExistingDatastore = manager.hasGrailsPlugin("hibernate")
        def foe = application?.config?.grails?.gorm?.failOnError
        methodsConfigurer.failOnError = foe instanceof Boolean ? foe : false
        
        methodsConfigurer.configure()
    }

    def doWithApplicationContext = { ctx ->
        new SimpleDBApplicationContextConfigurer().configure(ctx)
    }


    def onChange = { event ->
        def onChangeHandler = new SimpleDBOnChangeHandler()
        onChangeHandler.onChange(delegate, event)
    }

}
