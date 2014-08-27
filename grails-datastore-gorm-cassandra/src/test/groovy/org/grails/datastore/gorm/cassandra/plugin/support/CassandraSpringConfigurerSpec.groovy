package org.grails.datastore.gorm.cassandra.plugin.support

import grails.core.DefaultGrailsApplication
import grails.core.GrailsApplication
import grails.gorm.tests.Person
import grails.plugins.DefaultGrailsPluginManager
import grails.spring.BeanBuilder
import org.grails.core.artefact.DomainClassArtefactHandler
import org.grails.datastore.gorm.Setup
import org.grails.datastore.mapping.cassandra.config.CassandraMappingContext

import spock.lang.Specification

/**
 */
class CassandraSpringConfigurerSpec extends Specification{

    void "Test configure Cassandra via Spring for config"() {
        when:"The spring configurer is used"
            def configurer = new CassandraSpringConfigurer()
            def config = configurer.getConfiguration()
            def application = new DefaultGrailsApplication([Person] as Class[], Thread.currentThread().contextClassLoader)


            def keyspace = "randomKeyspaceTest"
			def contactPoints = "10.0.0.1"
            application.config.grails.cassandra.keyspace = keyspace
			application.config.grails.cassandra.contactPoints = contactPoints
			
            BeanBuilder bb = createInitialApplicationContext(application)
            bb.beans config

        then:"Then the cassandra beans have their config set"
            bb.getBeanDefinition('cassandraMappingContext').getPropertyValues().getPropertyValue('keyspace').value == keyspace
			def i = bb.getBeanDefinition('cassandraDatastore').getPropertyValues().getPropertyValue('config').value.get("contactPoints") == contactPoints		
            bb.getBeanDefinition('cassandraTemplate') != null
    }

    void "Test configure Cassandra via Spring"() {
        when:"The spring configurer is used"
            def configurer = new CassandraSpringConfigurer()
            def config = configurer.getConfiguration()
            final defaultConfig = {
                version false
            }
            def application = new DefaultGrailsApplication([Person] as Class[], Thread.currentThread().contextClassLoader)
			application.metadata.applicationName = Setup.keyspace
            BeanBuilder bb = createInitialApplicationContext(application, defaultConfig)
            bb.beans config
            def ctx = bb.createApplicationContext()
            CassandraMappingContext mappingContext = ctx.getBean("cassandraMappingContext",CassandraMappingContext)
            def entity = mappingContext?.getPersistentEntity(Person.name)
			
			
        then:"The application context is created"
            ctx != null
            ctx.containsBean("persistenceInterceptor")
			mappingContext.defaultMapping == defaultConfig
			entity != null
			def cassandraTemplate = ctx.getBean("cassandraTemplate", CassandraTemplate)
			cassandraTemplate != null
			cassandraTemplate.session != null
			def cluster = cassandraTemplate.session.cluster
			cluster != null			
            cluster.metadata.getKeyspace(Setup.keyspace) != null

    }

    protected BeanBuilder createInitialApplicationContext(GrailsApplication application, Closure defaultMappingConfig = {}) {
        def bb = new BeanBuilder()
        final binding = new Binding()

        application.config.grails.cassandra.default.mapping = defaultMappingConfig
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
