
import org.grails.datastore.gorm.support.DatastorePersistenceContextInterceptor
import org.springframework.datastore.mapping.web.support.OpenSessionInViewInterceptor
import org.springframework.datastore.mapping.transactions.DatastoreTransactionManager
import org.grails.datastore.gorm.GormInstanceApi
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.gorm.mongo.*
import org.grails.datastore.gorm.mongo.bean.factory.*
import org.springframework.data.document.mongodb.bean.factory.*

import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.springframework.datastore.mapping.reflect.ClassPropertyFetcher
import org.springframework.context.ApplicationContext
import org.springframework.datastore.mapping.core.Datastore
import org.springframework.transaction.PlatformTransactionManager
import org.grails.datastore.gorm.utils.InstanceProxy
import com.mongodb.ServerAddress

class MongodbGrailsPlugin {
    // the plugin version
    def version = "1.0.0.M1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.3.5 > *"
    // the other plugins this plugin depends on
    def dependsOn = [:]
    def loadAfter = ['domainClass']
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    def author = "Graeme Rocher"
    def authorEmail = "graeme.rocher@springsource.com"
    def title = "MongoDB GORM"
    def description = '''\\
A plugin that integrates the Mongo document datastore into Grails, providing
a GORM API onto it
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/mongodb"

    def doWithSpring = {
        def mongoConfig = application.config?.grails?.mongo


        mongoTransactionManager(DatastoreTransactionManager) {
          datastore = ref("mongoDatastore")
        }

		def databaseName = mongoConfig?.remove("databaseName") ?: application.metadata.getApplicationName()	
		"${databaseName}DB"(org.springframework.beans.factory.config.MethodInvokingFactoryBean) { bean ->
			bean.scope = "request"
			targetObject = ref("mongo")
			targetMethod = "getDB"
			arguments = [databaseName]
		}
		mongoMappingContext(MongoMappingContextFactoryBean) {
		  defaultDatabaseName = databaseName
          grailsApplication = ref('grailsApplication')
          pluginManager = ref('pluginManager')
        }

		mongoOptions(MongoOptionsFactoryBean) {
			if(mongoConfig?.options) {
				for(option in mongoConfig.remove("options")) {
					setProperty(option.key, option.value)
				}
			}
		}
		mongo(GMongoFactoryBean) {
			mongoOptions = mongoOptions
			def mongoHost = mongoConfig?.remove("host")
			if(mongoHost) {
				host = mongoHost
				def mongoPort = mongoConfig?.remove("port")
				if(mongoPort) port = mongoPort			
			} 
			else if(mongoConfig?.replicaPair) {
				def pair = []
				for(server in mongoConfig.remove("replicaPair")) {
						pair << new com.mongodb.DBAddress(server.indexOf("/")>0 ? server : "$server/$databaseName")
				}
				replicaPair = pair
			}	
			else if(mongoConfig?.replicaSet) {
				def set = []
				for(server in mongoConfig.remove("replicaSet")) {
						set << new com.mongodb.DBAddress(server.indexOf("/")>0 ? server : "$server/$databaseName")
				}
				
				replicaSetSeeds = set				
			}	
 		}
		mongoBean(mongo:"getMongo")
		mongoDatastore(MongoDatastoreFactoryBean) {
			mongo = ref("mongoBean")
			mappingContext = mongoMappingContext
			config = mongoConfig.toProperties()
		}
		
        mongoPersistenceInterceptor(DatastorePersistenceContextInterceptor, ref("mongoDatastore"))		

        if (manager?.hasGrailsPlugin("controllers")) {
            mongoOpenSessionInViewInterceptor(OpenSessionInViewInterceptor) {
                datastore = ref("mongoDatastore")
            }
            if (getSpringConfig().containsBean("controllerHandlerMappings")) {
                controllerHandlerMappings.interceptors << mongoOpenSessionInViewInterceptor
            }
            if (getSpringConfig().containsBean("annotationHandlerMapping")) {
                if (annotationHandlerMapping.interceptors) {
                    annotationHandlerMapping.interceptors << mongoOpenSessionInViewInterceptor
                }
                else {
                    annotationHandlerMapping.interceptors = [mongoOpenSessionInViewInterceptor]
                }
            }
        }
    }

    def doWithDynamicMethods = { ctx ->
	
      Datastore datastore = ctx.getBean("mongoDatastore")
      PlatformTransactionManager transactionManager = ctx.getBean("mongoTransactionManager")
      def enhancer = transactionManager ?
                          new MongoGormEnhancer(datastore, transactionManager) :
                          new MongoGormEnhancer(datastore)


   	  def isHibernateInstalled = manager.hasGrailsPlugin("hibernate")
      for(entity in datastore.mappingContext.persistentEntities) {
        def cls = entity.javaClass	
        def cpf = ClassPropertyFetcher.forClass(cls)	
        def mappedWith = cpf.getStaticPropertyValue(GrailsDomainClassProperty.MAPPING_STRATEGY, String)	
        if(isHibernateInstalled) {
          if(mappedWith == 'mongo') {
            enhancer.enhance(entity)
          }
          else {
            def staticApi = new MongoGormStaticApi(cls, datastore)
            def instanceApi = new GormInstanceApi(cls, datastore)
            cls.metaClass.static.getMongo = {-> staticApi }
            cls.metaClass.getMongo = {-> new InstanceProxy(instance:delegate, target:instanceApi) }
          }
        }
        else {
		  if(mappedWith == 'mongo' || mappedWith == null)
          	enhancer.enhance(entity)
        }
      }
    }


}
