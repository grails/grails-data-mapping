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
import org.springframework.datastore.mapping.model.PersistentEntity
import org.springframework.datastore.mapping.simpledb.engine.SimpleDBDomainResolver
import org.springframework.datastore.mapping.simpledb.engine.SimpleDBDomainResolverFactory
import org.springframework.datastore.mapping.simpledb.util.SimpleDBTemplate
import org.springframework.datastore.mapping.simpledb.util.SimpleDBConst

class SimpledbGrailsPlugin {
    def license = "Apache 2.0 License"
    def scm = [ url: "https://github.com/SpringSource/spring-data-mapping" ]
    def developers = [
        [ name: "Roman Stepanenko", email: "rs.opensource@gmail.com" ] ]
    def version = "1.0.0.M1"
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
        simpleDBDomainClassProcessor(application, manager, { dc ->
            String validatorBeanName = "${dc.fullName}Validator"
            def beandef = springConfig.getBeanConfig(validatorBeanName)?.beanDefinition ?:
                          springConfig.getBeanDefinition(validatorBeanName)
            beandef.beanClassName = GrailsDomainClassValidator.name
        })
    }

    /**
     * Iterates over all domain classes which are mapped with SimpleDB and passes them to the specified closure
     */
    def simpleDBDomainClassProcessor = { application, manager, closure ->
        def isHibernateInstalled = manager.hasGrailsPlugin("hibernate")
        for (dc in application.domainClasses) {
            def cls = dc.clazz
            def cpf = ClassPropertyFetcher.forClass(cls)
            def mappedWith = cpf.getStaticPropertyValue(GrailsDomainClassProperty.MAPPING_STRATEGY, String)
            if (mappedWith == SimpleDBConst.SIMPLE_DB_MAP_WITH_VALUE || (!isHibernateInstalled && mappedWith == null)) {
                closure.call(dc)
            }
        }
    }

    def doWithApplicationContext = { applicationContext ->
        //determine dbCreate flag and create/delete AWS domains if needed
        def simpleDBDomainClasses = []
        simpleDBDomainClassProcessor(application, manager, { dc ->
            simpleDBDomainClasses.add(dc) //collect domain classes which are stored via SimpleDB
        })
        def simpleDBConfig = application.config?.grails?.simpleDB
        handleDBCreate(simpleDBConfig.dbCreate,
                application,
                simpleDBDomainClasses,
                applicationContext.getBean("simpleDBMappingContext"),
                applicationContext.getBean("simpleDBDatastore")
        ); //similar to JDBC datastore, do 'create' or 'drop-create'
    }

    def handleDBCreate = { dbCreate, application, simpleDBDomainClasses, mappingContext, simpleDBDatastore ->
        boolean drop = false
        boolean create = false
        if ("drop-create" == dbCreate){
            drop = true
            create = true
        } else if ("create" == dbCreate){
            create = true
        }

        SimpleDBDomainResolverFactory resolverFactory = new SimpleDBDomainResolverFactory();
        for (dc in simpleDBDomainClasses) {
            def cls = dc.clazz
            PersistentEntity entity = mappingContext.getPersistentEntity(cls.getName())
            SimpleDBDomainResolver domainResolver = resolverFactory.buildResolver(entity, simpleDBDatastore);
            def domains = domainResolver.getAllDomainsForEntity()
            SimpleDBTemplate template = simpleDBDatastore.getSimpleDBTemplate(entity)

            if (drop){
                domains.each{ domain ->
                    template.deleteDomain (domain)
                }
            }
            if (create){
                domains.each{ domain ->
                    template.createDomain (domain)
                }
            }
        }
    }

    boolean shouldCreateTransactionalProxy(GrailsServiceClass serviceClass) {

        if (serviceClass.getStaticPropertyValue('transactional', Boolean)) {
            // leave it as a regular proxy
            return false
        }

        if (!SimpleDBConst.SIMPLE_DB_MAP_WITH_VALUE.equals(serviceClass.getStaticPropertyValue('transactional', String))) {
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
                if (mappedWith == SimpleDBConst.SIMPLE_DB_MAP_WITH_VALUE) {
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
                if (mappedWith == SimpleDBConst.SIMPLE_DB_MAP_WITH_VALUE || mappedWith == null) {
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
}
