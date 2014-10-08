import org.grails.datastore.gorm.cassandra.plugin.support.CassandraMethodsConfigurer
import org.grails.datastore.gorm.cassandra.plugin.support.CassandraSpringConfigurer

class CassandraGrailsPlugin {
	def license = "Apache 2.0 License"
	def organization = [name: "SpringSource", url: "http://www.springsource.org/"]
	def developers = [
		[name: "Jeff Beck", email: "beckje01@gmail.com"]]
	def issueManagement = [system: "todo", url: "todo"]
	def scm = [url: "https://github.com/grails/grails-data-mapping"]

	def version = "1.0.0-SNAPSHOT"
	def grailsVersion = "2.3.2 > *"
	def observe = ['services', 'domainClass']
	def loadAfter = ['domainClass', 'hibernate', 'hibernate4', 'services', 'cloudFoundry']
	def author = "Jeff Beck, Paras Lakhani"
	def authorEmail = "paras@atoms.to"
	def title = "Cassandra GORM"
	def description = 'A plugin that integrates the Cassandra datastore into Grails, providing a GORM API onto it'

	def documentation = ""

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
			//TODO create cassandraOnChangeHandler
			//            new MongoOnChangeHandler(event.ctx.mongoDatastore, event.ctx.mongoTransactionManager).onChange(delegate, event)
		}
	}
}
