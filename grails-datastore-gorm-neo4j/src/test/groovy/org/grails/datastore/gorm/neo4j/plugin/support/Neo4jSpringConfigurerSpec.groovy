package org.grails.datastore.gorm.neo4j.plugin.support

import grails.core.DefaultGrailsApplication
import grails.core.GrailsApplication
import grails.gorm.tests.Person
import grails.plugins.DefaultGrailsPluginManager
import grails.spring.BeanBuilder
import org.grails.core.artefact.DomainClassArtefactHandler
import spock.lang.Specification

/**
 * Test Neo4j configuration
 * @author Stefan Armbruster
 */
class Neo4jSpringConfigurerSpec extends Specification {

    def initialize(Closure closure) {
        def configurer = new Neo4jSpringConfigurer()
        def config = configurer.getConfiguration()
        def application = new DefaultGrailsApplication([Person] as Class[], Thread.currentThread().contextClassLoader)

        closure.delegate = application.config.grails.neo4j
        closure()
//        application.config.grails.neo4j.url = connectionString
        BeanBuilder bb = createInitialApplicationContext(application)
        bb.beans config
        bb
    }

    void "empty configuration uses test graph database"() {
        when: "The spring configurer is used"
        def bb = initialize {}

        then:
        bb.getBeanDefinition("neo4jPoolConfiguration").propertyValues.getPropertyValue("url").value== "jdbc:neo4j:mem"

        and:
        bb.getBeanDefinition('cypherEngine')

        and: "no explicit graphDatabaseService defined"
        !bb.getBeanDefinition("graphDatabaseService")
    }

    void "test embedded db configuration for single instance"() {
        when:
        def connectionString = "jdbc:neo4j:instance:dummy"
        def bb = initialize {
            url = connectionString
            dbProperties = [
                    allow_store_upgrade: true
            ]
        }

        then:
        bb.getBeanDefinition("graphDatabaseService")

        and:
        bb.getBeanDefinition("graphDbFactory").beanClassName == "org.neo4j.graphdb.factory.GraphDatabaseFactory"

        and:
        bb.getBeanDefinition("neo4jDataSource").constructorArgumentValues.genericArgumentValues[0].value.dbProperties.dummy.beanName == "graphDatabaseService"
        bb.getBeanDefinition("neo4jDataSource").constructorArgumentValues.genericArgumentValues[0].value.dbProperties.allow_store_upgrade == true

    }

    void "test high availabile db configuration "() {
        when:
        def connectionString = "jdbc:neo4j:instance:dummy"
        def bb = initialize {
            url = connectionString
            ha = true
            dbProperties = [
                    allow_store_upgrade: true
            ]
        }

        then:
        bb.getBeanDefinition("graphDatabaseService")

        and:
        bb.getBeanDefinition("graphDbFactory").beanClassName == "org.neo4j.graphdb.factory.HighlyAvailableGraphDatabaseFactory"

        and:
        bb.getBeanDefinition("neo4jDataSource").constructorArgumentValues.genericArgumentValues[0].value.dbProperties.dummy.beanName == "graphDatabaseService"
        bb.getBeanDefinition("neo4jDataSource").constructorArgumentValues.genericArgumentValues[0].value.dbProperties.allow_store_upgrade == true

    }

    protected BeanBuilder createInitialApplicationContext(GrailsApplication application, Closure defaultMappingConfig = {}) {
        def bb = new BeanBuilder()
        final binding = new Binding()

//        application.config.grails.mongo.databaseName = "test"
//        application.config.grails.mongo.default.mapping = defaultMappingConfig
        application.initialise()
        application.registerArtefactHandler(new DomainClassArtefactHandler())
        binding.setVariable("application", application)
        binding.setVariable("manager", new DefaultGrailsPluginManager([] as Class[], application))
        bb.setBinding(binding)
        bb.beans {
            grailsApplication(DefaultGrailsApplication, [Person] as Class[], Thread.currentThread().contextClassLoader) { bean ->
                bean.initMethod = "initialise"
            }
            pluginManager(DefaultGrailsPluginManager, [] as Class[], ref("grailsApplication"))
        }

        return bb
    }

}
