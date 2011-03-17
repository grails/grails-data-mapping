import com.mongodb.DBAddress
import com.mongodb.ServerAddress

import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty

import org.grails.datastore.gorm.GormInstanceApi
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.gorm.mongo.*
import org.grails.datastore.gorm.mongo.bean.factory.*
import org.grails.datastore.gorm.support.DatastorePersistenceContextInterceptor
import org.grails.datastore.gorm.utils.InstanceProxy

import org.springframework.beans.factory.config.MethodInvokingFactoryBean
import org.springframework.context.ApplicationContext
import org.springframework.data.document.mongodb.bean.factory.*
import org.springframework.datastore.mapping.core.Datastore
import org.springframework.datastore.mapping.reflect.ClassPropertyFetcher
import org.springframework.datastore.mapping.transactions.DatastoreTransactionManager
import org.springframework.datastore.mapping.web.support.OpenSessionInViewInterceptor
import org.springframework.transaction.PlatformTransactionManager

class MongodbGrailsPlugin {
    def license = "Apache 2.0 License"
    def organization = [ name: "SpringSource", url: "http://www.springsource.org/" ]
    def developers = [
        [ name: "Graeme Rocher", email: "grocher@vmware.com" ] ]
    def issueManagement = [ system: "JIRA", url: "http://jira.codehaus.org/browse/GRAILSPLUGINS" ]
    def scm = [ url: "https://github.com/grails/inconsequential" ]

    def version = "1.0-M2"
    def grailsVersion = "1.3.5 > *"
    def loadAfter = ['domainClass']
    def author = "Graeme Rocher"
    def authorEmail = "graeme.rocher@springsource.com"
    def title = "MongoDB GORM"
    def description = '''\\
A plugin that integrates the Mongo document datastore into Grails, providing
a GORM API onto it
'''

    def documentation = "http://grails.org/plugin/mongodb"

    def doWithSpring = {
        def mongoConfig = application.config?.grails?.mongo

        mongoTransactionManager(DatastoreTransactionManager) {
            datastore = ref("mongoDatastore")
        }

        def databaseName = mongoConfig?.remove("databaseName") ?: application.metadata.getApplicationName()
        "${databaseName}DB"(MethodInvokingFactoryBean) { bean ->
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
            if (mongoConfig?.options) {
                for (option in mongoConfig.remove("options")) {
                    setProperty(option.key, option.value)
                }
            }
        }

        mongo(GMongoFactoryBean) {
            mongoOptions = mongoOptions
            def mongoHost = mongoConfig?.remove("host")
            if (mongoHost) {
                host = mongoHost
                def mongoPort = mongoConfig?.remove("port")
                if (mongoPort) port = mongoPort
            }
            else if (mongoConfig?.replicaPair) {
                def pair = []
                for (server in mongoConfig.remove("replicaPair")) {
                    pair << new DBAddress(server.indexOf("/")>0 ? server : "$server/$databaseName")
                }
                replicaPair = pair
            }
            else if (mongoConfig?.replicaSet) {
                def set = []
                for (server in mongoConfig.remove("replicaSet")) {
                    set << new DBAddress(server.indexOf("/")>0 ? server : "$server/$databaseName")
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

        Datastore datastore = ctx.mongoDatastore
        PlatformTransactionManager transactionManager = ctx.mongoTransactionManager
        def enhancer = transactionManager ?
            new MongoGormEnhancer(datastore, transactionManager) :
            new MongoGormEnhancer(datastore)

        def isHibernateInstalled = manager.hasGrailsPlugin("hibernate")
        for (entity in datastore.mappingContext.persistentEntities) {
            def cls = entity.javaClass
            def cpf = ClassPropertyFetcher.forClass(cls)
            def mappedWith = cpf.getStaticPropertyValue(GrailsDomainClassProperty.MAPPING_STRATEGY, String)
            if (isHibernateInstalled) {
                if (mappedWith == 'mongo') {
                    enhancer.enhance(entity)
                }
                else {
                    def staticApi = new MongoGormStaticApi(cls, datastore)
                    def instanceApi = new MongoGormInstanceApi(cls, datastore)
                    cls.metaClass.static.getMongo = {-> staticApi }
                    cls.metaClass.getMongo = {-> new InstanceProxy(instance:delegate, target:instanceApi) }
                }
            }
            else {
                if (mappedWith == 'mongo' || mappedWith == null) {
                    enhancer.enhance(entity)
                }
            }
        }
    }
}
