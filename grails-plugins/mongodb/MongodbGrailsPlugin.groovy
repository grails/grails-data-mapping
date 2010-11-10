
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

        def existingTransactionManager = manager?.hasGrailsPlugin("hibernate") || getSpringConfig().containsBean("transactionManager")
        def txManagerName = existingTransactionManager ? 'mongoTransactionManager' : 'transactionManager'

        "$txManagerName"(DatastoreTransactionManager) {
          datastore = ref("mongoDatastore")
        }

		mongoMappingContext(MongoMappingContextFactoryBean) {
		  defaultDatabaseName = mongoConfig?.defaultDatabase ?: application.metadata.getApplicationName()	
          grailsApplication = ref('grailsApplication')
          pluginManager = ref('pluginManager')
        }

		mongoOptions(MongoOptionsFactoryBean) {
			if(mongoConfig?.options) {
				for(option in mongoConfig?.options) {
					delegate."${option.key}" = option.value
				}
			}
		}
		mongo(GMongoFactoryBean) {
			mongoOptions = mongoOptions
			if(mongoConfig?.host) host = mongoConfig?.host
			if(mongoConfig?.port) host = mongoConfig?.port			
			if(mongoConfig?.replicaSet) {
				def set = []
				for(server in mongoConfig?.replicaSet) {
					if(server.host && server.port)
						set << new com.mongodb.ServerAddress(server.host, server.port)
					else
						set << new com.mongodb.ServerAddress(server.host)
				}
				replicaSetSeeds = set
			}
		}
		mongoDatastore(MongoDatastoreFactoryBean) {
			mongo = ref("mongo")
			mappingContext = mongoMappingContext
		}
		
        mongoPersistenceInterceptor(DatastorePersistenceContextInterceptor, ref("mongoDatastore"))		

        if (manager?.hasGrailsPlugin("controllers")) {
            datastoreOpenSessionInViewInterceptor(OpenSessionInViewInterceptor) {
                datastore = ref("springDatastore")
            }
            if (getSpringConfig().containsBean("controllerHandlerMappings")) {
                controllerHandlerMappings.interceptors << datastoreOpenSessionInViewInterceptor
            }
            if (getSpringConfig().containsBean("annotationHandlerMapping")) {
                if (annotationHandlerMapping.interceptors) {
                    annotationHandlerMapping.interceptors << datastoreOpenSessionInViewInterceptor
                }
                else {
                    annotationHandlerMapping.interceptors = [datastoreOpenSessionInViewInterceptor]
                }
            }
        }
    }

    def doWithDynamicMethods = { ctx ->
      Datastore datastore = ctx.getBean(Datastore)
      PlatformTransactionManager transactionManager = ctx.getBean(DatastoreTransactionManager)
      def enhancer = transactionManager ?
                          new MongoGormEnhancer(datastore, transactionManager) :
                          new MongoGormEnhancer(datastore)


   	  def isHibernateInstalled = manager.hasGrailsPlugin("hibernate")
      for(entity in datastore.mappingContext.persistentEntities) {
        def mappedWith = cpf.getStaticPropertyValue(GrailsDomainClassProperty.MAPPING_STRATEGY, String)	
        if(isHibernateInstalled) {
          def cls = entity.javaClass
          def cpf = ClassPropertyFetcher.forClass(cls)

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
