import org.grails.datastore.gorm.cassandra.plugin.support.CassandraMethodsConfigurer
import org.grails.datastore.gorm.cassandra.plugin.support.CassandraSpringConfigurer

class CassandraGrailsPlugin {
	def license = "Apache 2.0 License"
	def organization = [name: "SpringSource", url: "http://www.springsource.org/"]
	def developers = [
		[name: "Jeff Beck", email: "beckje01@gmail.com"],
		[name: "Paras Lakhani", email: "paras@atoms.to"]
	]
	
	def scm = [url: "https://github.com/grails/grails-data-mapping"]

	def version = "1.0.0-M01"
	def grailsVersion = "2.3.2 > *"
	def observe = ['services', 'domainClass']
	def loadAfter = ['domainClass', 'hibernate', 'hibernate4', 'services', 'cloudFoundry']	
	def title = "Cassandra GORM"
	def description = 'A plugin that integrates the Cassandra datastore into Grails, providing a GORM API onto it'

	def documentation = "http://grails.github.io/grails-data-mapping/cassandra"

	def doWithSpring = new CassandraSpringConfigurer().getConfiguration()


	def doWithDynamicMethods = { ctx ->
		def datastore = ctx.cassandraDatastore
		def transactionManager = ctx.TransactionManager
		def methodsConfigurer = new CassandraMethodsConfigurer(datastore, transactionManager)
		methodsConfigurer.hasExistingDatastore = manager.hasGrailsPlugin("hibernate") || manager.hasGrailsPlugin("hibernate4")
		def foe = application?.config?.grails?.gorm?.failOnError
		methodsConfigurer.failOnError = foe instanceof Boolean ? foe : false
		methodsConfigurer.configure()
	}
    
	def onChange = { event ->
		if (event.ctx) {
			
		}
	}
}
