package org.grails.datastore.gorm.cassandra.plugin.support

import grails.gorm.tests.Person
import grails.spring.BeanBuilder

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.plugins.DefaultGrailsPluginManager
import org.grails.datastore.mapping.cassandra.CassandraDatastore
import org.grails.datastore.mapping.cassandra.config.CassandraMappingContext
import org.springframework.data.cassandra.core.CassandraTemplate

import spock.lang.Specification

import com.datastax.driver.core.KeyspaceMetadata

/**
 */
class CassandraSpringConfigurerSpec extends Specification{

    void "Test configure Cassandra via Spring for config"() {
		given: 
    		def configurer = new CassandraSpringConfigurer()
    		def config = configurer.getConfiguration()
    		def application = new DefaultGrailsApplication([Person] as Class[], Thread.currentThread().contextClassLoader)
    		def keyspace = randomKeyspaceName()
    		def contactPoints = "10.0.0.1"
    		application.config.grails.cassandra.keyspace.name = keyspace
    		application.config.grails.cassandra.contactPoints = contactPoints
			
        when:"The spring configurer is used"		            	
            BeanBuilder bb = createInitialApplicationContext(application)
            bb.beans config

        then:"Then the cassandra beans have their config set"
            bb.getBeanDefinition('cassandraMappingContext').getPropertyValues().getPropertyValue('keyspace').value == keyspace
			def i = bb.getBeanDefinition('cassandraDatastore').getPropertyValues().getPropertyValue('config').value.get("contactPoints") == contactPoints		
            bb.getBeanDefinition('cassandraTemplate') != null											
    }

    void "Test configure Cassandra keyspace via Spring"() {
		given:
    		def configurer = new CassandraSpringConfigurer()
    		def config = configurer.getConfiguration()
    		def keyspace = randomKeyspaceName()
    		final defaultConfig = {
    			version false
    		}
    		def application = new DefaultGrailsApplication([Person] as Class[], Thread.currentThread().contextClassLoader)
    		application.config.grails.cassandra.keyspace.name = keyspace
    		application.config.grails.cassandra.keyspace.action = "CREATE_DROP"
			application.config.grails.cassandra.keyspace.durableWrites = false			
			application.config.grails.cassandra.keyspace.replicationStrategy = "NetworkTopologyStrategy"
			application.config.grails.cassandra.keyspace.networkTopology = ["us-east":1, "eu-west":2]
			
        when:"The spring configurer is used"		           
            BeanBuilder bb = createInitialApplicationContext(application, defaultConfig)
            bb.beans config
            def ctx = bb.createApplicationContext()
            CassandraMappingContext mappingContext = ctx.getBean("cassandraMappingContext",CassandraMappingContext)
            def entity = mappingContext?.getPersistentEntity(Person.name)
				
        then:"The application context cassandra beans and keyspace is created"
            ctx != null
            ctx.containsBean("persistenceInterceptor")
			mappingContext.defaultMapping == defaultConfig
			entity != null
			def cassandraTemplate = ctx.getBean("cassandraTemplate", CassandraTemplate)
			cassandraTemplate != null
			cassandraTemplate.session != null
			def cassandraDatastore = ctx.getBean("cassandraDatastore")
			def cluster = cassandraDatastore.nativeCluster
			cluster != null			
            KeyspaceMetadata keyspaceMeta = cluster.metadata.getKeyspace(keyspace)
			keyspaceMeta != null
			keyspaceMeta.name == keyspace						
			keyspaceMeta.durableWrites == false
			keyspaceMeta.replication['class'] == "org.apache.cassandra.locator.NetworkTopologyStrategy"						
			
		when:"CassandraDatastore created with keyspace already created"
			def cassandraDatastore2 = new CassandraDatastore(mappingContext, [:], ctx)
			cassandraDatastore2.afterPropertiesSet()
		
		then:"CassandraDatastore does not create a keyspace"
			cassandraDatastore2.keyspaceActionSpecificationFactoryBean == null
			def cluster2 = cassandraDatastore2.nativeCluster
			cluster2 != null
			def keyspaceMeta2 = cluster2.metadata.getKeyspace(keyspace)
			keyspaceMeta2 != null
			keyspaceMeta2.name == keyspace
			cassandraDatastore.destroy() //instance that contains the keyspace destroy specification
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
	
	def String randomKeyspaceName() {
		return "ks" + UUID.randomUUID().toString().replace("-", "")
	}
}
