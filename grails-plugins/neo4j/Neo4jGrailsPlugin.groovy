import org.grails.datastore.gorm.neo4j.plugin.support.Neo4jMethodsConfigurer
import org.grails.datastore.gorm.neo4j.plugin.support.Neo4jOnChangeHandler
import org.grails.datastore.gorm.neo4j.plugin.support.Neo4jSpringConfigurer
import org.neo4j.kernel.impl.core.NodeProxy
import org.neo4j.kernel.impl.core.RelationshipProxy
import grails.converters.JSON
import org.codehaus.groovy.grails.commons.metaclass.MetaClassEnhancer
import org.codehaus.groovy.grails.plugins.converters.api.ConvertersApi
import org.codehaus.groovy.grails.commons.GrailsMetaClassUtils
import org.springframework.context.ApplicationContext

class Neo4jGrailsPlugin {

    def license = "Apache 2.0 License"
    def organization = [ name: "Stefan Armbruster", url: "http://blog.armbruster-it.de/" ]
    def developers = [
        [ name: "Stefan Armbruster", email: "stefan@armbruster-it.de" ] ]
    def issueManagement = [ system: "JIRA", url: "https://github.com/grails/grails-data-mapping/issues" ]
    def scm = [ url: "https://github.com/grails/grails-data-mapping" ]

    def version = "2.0.0-M02"
//    def version = "2.0.0-SNAPSHOT"
    def grailsVersion = "2.3 > *"
    def loadAfter = ['domainClass', 'hibernate', 'services', 'cloudFoundry', 'converters']
    def loadBefore = ['dataSource']
    def observe = ['services', 'domainClass']
        
    def author = "Stefan Armbruster"
    def authorEmail = "stefan@armbruster-it.de"
    def title = "Neo4j GORM"    
    def description = 'A plugin that integrates the Neo4j graph database into Grails, providing a GORM API onto it'

    def documentation = "http://projects.spring.io/grails-data-mapping/neo4j/manual/index.html"

    def dependsOn = [:]
    // resources that are excluded from plugin packaging

    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    def doWithSpring = new Neo4jSpringConfigurer().getConfiguration()

    def doWithDynamicMethods = { ctx ->
        def datastore = ctx.neo4jDatastore
        def transactionManager = ctx.neo4jTransactionManager
        def methodsConfigurer = new Neo4jMethodsConfigurer(datastore, transactionManager)
        methodsConfigurer.hasExistingDatastore = manager.hasGrailsPlugin("hibernate")
        def foe = application?.config?.grails?.gorm?.failOnError
        methodsConfigurer.failOnError = foe instanceof Boolean ? foe : false

        methodsConfigurer.configure()

        setupGetOrSet()

        setupJsonMarshallers(ctx)
    }

    private void setupJsonMarshallers(ApplicationContext ctx) {
        MetaClassEnhancer enhancer = new MetaClassEnhancer()
        enhancer.addApi(new ConvertersApi(applicationContext: ctx))

        // Override GDK asType for some common Interfaces and Classes
        enhancer.enhanceAll([NodeProxy, RelationshipProxy].collect {
            GrailsMetaClassUtils.getExpandoMetaClass(it)
        })


        JSON.registerObjectMarshaller(NodeProxy) { n ->
            def m = [:]
            m.id = n.id
            n.propertyKeys.each { k ->
                m[(k)] = n."$k"
            }
            m.relationships = n.relationships.collect {it}
            m
        }

        JSON.registerObjectMarshaller(RelationshipProxy) { r ->
            def m = [:]
            m.id = r.id
            m.type = r.type.name()
            m.startNode = r.startNode.id
            m.endNode = r.endNode.id
            r.propertyKeys.each { k ->
                m[(k)] = r."$k"
            }
            m
        }
    }

    private void setupGetOrSet() {
        def getOrSet = { name, value = null ->
            if (value) {
                delegate.setProperty(name, value instanceof Date ? value.time : value)
            } else {
                def val = delegate.getProperty(name, null)
                (val instanceof Long && name.endsWith('Date')) ? new Date(val) : val
            }
        }

        def classLoader = Thread.currentThread().contextClassLoader
        def classes = [NodeProxy, RelationshipProxy]
        ['org.neo4j.rest.graphdb.entity.RestNode', 'org.neo4j.rest.graphdb.entity.RestRelationship'].each {
            try {
                classes << classLoader.loadClass(it)
            } catch (ClassNotFoundException e) {
                //pass
            }
        }

        classes.each {
            it.metaClass.propertyMissing = getOrSet
        }
    }

    def doWithApplicationContext = { applicationContext ->
    }

    def onChange = { event ->
        if(event.ctx) {
            new Neo4jOnChangeHandler(event.ctx.neo4jDatastore, event.ctx.neo4jTransactionManager).onChange(delegate, event)            
        }
    }

}
