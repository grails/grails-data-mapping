import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.commons.GrailsServiceClass
import org.codehaus.groovy.grails.commons.ServiceArtefactHandler
import org.codehaus.groovy.grails.commons.spring.TypeSpecifyableTransactionProxyFactoryBean
import org.codehaus.groovy.grails.orm.support.GroovyAwareNamedTransactionAttributeSource
import org.codehaus.groovy.grails.validation.GrailsDomainClassValidator

import org.grails.datastore.gorm.support.DatastorePersistenceContextInterceptor
import org.grails.datastore.gorm.utils.InstanceProxy

import org.springframework.core.annotation.AnnotationUtils
import org.springframework.datastore.mapping.core.Datastore
import org.springframework.datastore.mapping.reflect.ClassPropertyFetcher
import org.springframework.datastore.mapping.transactions.DatastoreTransactionManager
import org.springframework.datastore.mapping.web.support.OpenSessionInViewInterceptor
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import org.grails.plugin.simpledb.PersistenceContextInterceptorAggregator
import java.lang.reflect.Method
import org.grails.datastore.gorm.simpledb.SimpleDBGormEnhancer
import org.grails.datastore.gorm.simpledb.SimpleDBGormStaticApi
import org.grails.datastore.gorm.simpledb.SimpleDBGormInstanceApi
import org.grails.datastore.gorm.simpledb.bean.factory.SimpleDBMappingContextFactoryBean
import org.grails.datastore.gorm.simpledb.bean.factory.SimpleDBDatastoreFactoryBean

class SimpledbGrailsPlugin {
    def license = "Apache 2.0 License"
    def scm = [ url: "https://github.com/SpringSource/spring-data-mapping" ]
    def developers = [
        [ name: "Roman Stepanenko", email: "rs.opensource@gmail.com" ] ]
    def version = "0.1"
    def grailsVersion = "1.3.5 > *"
    def observe = ['services']
    def loadAfter = ['domainClass', 'hibernate', 'services', 'cloudFoundry']
    def dependsOn = [:]
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    def author = "Roman Stepanenko"
    def authorEmail = "rs.opensource@gmail.com"
    def title = "SimpleDB GORM"
    def description = '''\\
This pluging allows to use standard GORM mapping to persist in AWS SimpleDB instead of going through JPA+SimpleJPA plugins.
This plugin will be evolved into full-fledged SimpleDB support with automatic sharding, ID generation,
configurable large String storage (in S3 or by splitting into attributes),
customizable performance tweaks according to SimpleDB best practices (dedicated columns for 'dummy null' for quick lookup by null values) etc.  
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/simpledb"

    def doWithSpring = {
        def simpleDBConfig = application.config?.grails?.simpleDB

        simpleDBTransactionManager(DatastoreTransactionManager) {
            datastore = ref("simpleDBDatastore")
        }

        simpleDBMappingContext(SimpleDBMappingContextFactoryBean) {
            grailsApplication = ref('grailsApplication')
            pluginManager = ref('pluginManager')
        }

//        simpleDBDatastore(SimpleDBDatastore, ref("simpleDBMappingContext"), simpleDBConfig.toProperties(), )
        simpleDBDatastore(SimpleDBDatastoreFactoryBean) {
            mappingContext = ref("simpleDBMappingContext")
            config = simpleDBConfig.toProperties()
        }

  
        simpleDBPersistenceInterceptor(DatastorePersistenceContextInterceptor, ref("simpleDBDatastore"))

        simpleDBPersistenceContextInterceptorAggregator(PersistenceContextInterceptorAggregator)

        if (manager?.hasGrailsPlugin("controllers")) {
            simpleDBOpenSessionInViewInterceptor(OpenSessionInViewInterceptor) {
                datastore = ref("simpleDBDatastore")
            }
            if (getSpringConfig().containsBean("controllerHandlerMappings")) {
                controllerHandlerMappings.interceptors << simpleDBOpenSessionInViewInterceptor
            }
            if (getSpringConfig().containsBean("annotationHandlerMapping")) {
                if (annotationHandlerMapping.interceptors) {
                    annotationHandlerMapping.interceptors << simpleDBOpenSessionInViewInterceptor
                }
                else {
                    annotationHandlerMapping.interceptors = [simpleDBOpenSessionInViewInterceptor]
                }
            }
        }

        // need to fix the service proxies to use simpleDBTransactionManager
        for (serviceGrailsClass in application.serviceClasses) {
            GrailsServiceClass serviceClass = serviceGrailsClass

            if (!shouldCreateTransactionalProxy(serviceClass)) {
                continue
            }

            def beanName = serviceClass.propertyName
            if (springConfig.containsBean(beanName)) {
                delegate."$beanName".transactionManager = ref('simpleDBTransactionManager')
            }
        }

        // make sure validators for SimpleDB domain classes are regular GrailsDomainClassValidator
        def isHibernateInstalled = manager.hasGrailsPlugin("hibernate")
        for (dc in application.domainClasses) {
            def cls = dc.clazz
            def cpf = ClassPropertyFetcher.forClass(cls)
            def mappedWith = cpf.getStaticPropertyValue(GrailsDomainClassProperty.MAPPING_STRATEGY, String)
            if (mappedWith == 'simpleDB' || (!isHibernateInstalled && mappedWith == null)) {
                String validatorBeanName = "${dc.fullName}Validator"
                def beandef = springConfig.getBeanConfig(validatorBeanName)?.beanDefinition ?:
                              springConfig.getBeanDefinition(validatorBeanName)
                beandef.beanClassName = GrailsDomainClassValidator.name
            }
        }
    }

    def doWithApplicationContext = { applicationContext ->
        // TODO Implement post initialization spring config (optional)
    }

    boolean shouldCreateTransactionalProxy(GrailsServiceClass serviceClass) {

        if (serviceClass.getStaticPropertyValue('transactional', Boolean)) {
            // leave it as a regular proxy
            return false
        }

        if (!'simpledb'.equals(serviceClass.getStaticPropertyValue('transactional', String))) {
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
        Datastore datastore = ctx.simpleDBDatastore
        PlatformTransactionManager transactionManager = ctx.simpleDBTransactionManager
        def enhancer = transactionManager ?
            new SimpleDBGormEnhancer(datastore, transactionManager) :
            new SimpleDBGormEnhancer(datastore)

        def isHibernateInstalled = manager.hasGrailsPlugin("hibernate")
        for (entity in datastore.mappingContext.persistentEntities) {
            def cls = entity.javaClass
            def cpf = ClassPropertyFetcher.forClass(cls)
            def mappedWith = cpf.getStaticPropertyValue(GrailsDomainClassProperty.MAPPING_STRATEGY, String)
            if (isHibernateInstalled) {
                if (mappedWith == 'simpledb') {
                    enhancer.enhance(entity)
                }
                else {
                    def staticApi = new SimpleDBGormStaticApi(cls, datastore, enhancer.finders)
                    def instanceApi = new SimpleDBGormInstanceApi(cls, datastore)
                    cls.metaClass.static.getSimpleDB = {-> staticApi }
                    cls.metaClass.getSimpleDB = {-> new InstanceProxy(instance:delegate, target:instanceApi) }
                }
            }
            else {
                if (mappedWith == 'simpledb' || mappedWith == null) {
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
                transactionManager = ref('simpleDBTransactionManager')
            }
        }
        beans.registerBeans(event.ctx)
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }
}
