package grails.plugins.cassandra

import grails.cassandra.bootstrap.CassandraDatastoreSpringInitializer
import grails.core.GrailsClass
import grails.plugins.Plugin
import org.grails.core.artefact.DomainClassArtefactHandler
import org.springframework.beans.factory.support.BeanDefinitionRegistry

class CassandraGrailsPlugin extends Plugin {
	def license = "Apache 2.0 License"
	def organization = [name: "SpringSource", url: "http://www.springsource.org/"]
	def developers = [
		[name: "Jeff Beck", email: "beckje01@gmail.com"],
		[name: "Paras Lakhani", email: "paras@atoms.to"]
	]
	
	def scm = [url: "https://github.com/grails/grails-data-mapping"]

	def grailsVersion = "2.3.2 > *"
	def observe = ['services', 'domainClass']
	def loadAfter = ['domainClass', 'hibernate', 'hibernate4', 'services', 'cloudFoundry']	
	def title = "Cassandra GORM"
	def description = 'A plugin that integrates the Cassandra datastore into Grails, providing a GORM API onto it'

	def documentation = "http://grails.github.io/grails-data-mapping/latest/cassandra"

	@Override
	Closure doWithSpring() {
		def initializer = new CassandraDatastoreSpringInitializer(config, grailsApplication.getArtefacts(DomainClassArtefactHandler.TYPE).collect() { GrailsClass cls -> cls.clazz })
		initializer.registerApplicationIfNotPresent = false
		return initializer.getBeanDefinitions((BeanDefinitionRegistry)applicationContext)
	}
}
