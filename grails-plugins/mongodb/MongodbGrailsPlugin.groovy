import java.lang.reflect.Method

import com.mongodb.DBAddress
import com.mongodb.ServerAddress

import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.commons.GrailsServiceClass
import org.codehaus.groovy.grails.commons.ServiceArtefactHandler
import org.codehaus.groovy.grails.commons.spring.TypeSpecifyableTransactionProxyFactoryBean
import org.codehaus.groovy.grails.orm.support.GroovyAwareNamedTransactionAttributeSource

import org.grails.datastore.gorm.GormInstanceApi
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.gorm.mongo.*
import org.grails.datastore.gorm.mongo.bean.factory.*
import org.grails.datastore.gorm.support.DatastorePersistenceContextInterceptor
import org.grails.datastore.gorm.utils.InstanceProxy

import org.springframework.beans.factory.config.MethodInvokingFactoryBean
import org.springframework.context.ApplicationContext
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.data.document.mongodb.bean.factory.*
import org.springframework.datastore.mapping.core.Datastore
import org.springframework.datastore.mapping.reflect.ClassPropertyFetcher
import org.springframework.datastore.mapping.transactions.DatastoreTransactionManager
import org.springframework.datastore.mapping.web.support.OpenSessionInViewInterceptor
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional

class MongodbGrailsPlugin {
    def license = "Apache 2.0 License"
    def organization = [ name: "SpringSource", url: "http://www.springsource.org/" ]
    def developers = [
        [ name: "Graeme Rocher", email: "grocher@vmware.com" ] ]
    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPMONGODB" ]
    def scm = [ url: "https://github.com/SpringSource/spring-data-mapping" ]

    def version = "1.0-M5"
    def grailsVersion = "1.3.5 > *"
    def observe = ['services']
    def loadAfter = ['domainClass', 'services']
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

        // need to redefine the service proxies to use mongoTransactionManager
        for (serviceGrailsClass in application.serviceClasses) {
            GrailsServiceClass serviceClass = serviceGrailsClass

            if (!shouldCreateTransactionalProxy(serviceClass)) {
                continue
            }

            def scope = serviceClass.getPropertyValue("scope")
            def props = ["*": "PROPAGATION_REQUIRED"] as Properties
            "${serviceClass.propertyName}"(TypeSpecifyableTransactionProxyFactoryBean, serviceClass.clazz) { bean ->
                if (scope) bean.scope = scope
                bean.lazyInit = true
                target = { innerBean ->
                    innerBean.lazyInit = true
                    innerBean.factoryBean = "${serviceClass.fullName}ServiceClass"
                    innerBean.factoryMethod = "newInstance"
                    innerBean.autowire = "byName"
                    if (scope) innerBean.scope = scope
                }
                proxyTargetClass = true
                transactionAttributeSource = new GroovyAwareNamedTransactionAttributeSource(transactionalAttributes:props)
                transactionManager = ref('mongoTransactionManager')
            }
        }
    }

    boolean shouldCreateTransactionalProxy(GrailsServiceClass serviceClass) {

        if (serviceClass.getStaticPropertyValue('transactional', Boolean)) {
            // leave it as a regular proxy
            return false
        }

        if (!'mongo'.equals(serviceClass.getStaticPropertyValue('transactional', String))) {
            return false
        }

        try {
            Class javaClass = serviceClass.clazz
            serviceClass.transactional &&
                !AnnotationUtils.findAnnotation(javaClass, Transactional) &&
                !javaClass.methods.any { Method m -> AnnotationUtils.findAnnotation(m, Transactional) != null }
        }
        catch (e) {
            return false
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

    def onChange = { event ->
        if (!event.source || !event.ctx) {
            return
        }

        def serviceClass = application.addArtefact(ServiceArtefactHandler.TYPE, event.source)
        if (!shouldCreateTransactionalProxy(serviceClass)) {
            return
        }

        def beans = beans {
            def scope = serviceClass.getPropertyValue("scope")
            def props = ["*": "PROPAGATION_REQUIRED"] as Properties
            "${serviceClass.propertyName}"(TypeSpecifyableTransactionProxyFactoryBean, serviceClass.clazz) { bean ->
                if (scope) bean.scope = scope
                bean.lazyInit = true
                target = { innerBean ->
                    innerBean.factoryBean = "${serviceClass.fullName}ServiceClass"
                    innerBean.factoryMethod = "newInstance"
                    innerBean.autowire = "byName"
                    if (scope) innerBean.scope = scope
                }
                proxyTargetClass = true
                transactionAttributeSource = new GroovyAwareNamedTransactionAttributeSource(transactionalAttributes:props)
                transactionManager = ref('mongoTransactionManager')
            }
        }
        beans.registerBeans(event.ctx)
    }
}
