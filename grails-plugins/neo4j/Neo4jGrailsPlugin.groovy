import org.neo4j.kernel.EmbeddedGraphDatabase
import org.grails.datastore.gorm.neo4j.bean.factory.Neo4jDatastoreFactoryBean
import org.grails.datastore.gorm.neo4j.bean.factory.Neo4jMappingContextFactoryBean
import org.springframework.datastore.mapping.core.Datastore
import org.springframework.transaction.PlatformTransactionManager
import org.grails.datastore.gorm.neo4j.Neo4jGormEnhancer
import org.springframework.datastore.mapping.reflect.ClassPropertyFetcher
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.gorm.GormInstanceApi
import org.grails.datastore.gorm.utils.InstanceProxy
import org.codehaus.groovy.grails.validation.GrailsDomainClassValidator
import org.grails.datastore.gorm.support.DatastorePersistenceContextInterceptor
import org.springframework.datastore.mapping.transactions.DatastoreTransactionManager
import org.springframework.datastore.mapping.web.support.OpenSessionInViewInterceptor
import org.codehaus.groovy.grails.commons.GrailsServiceClass
import org.grails.datastore.gorm.neo4j.Neo4jOpenSessionInViewInterceptor


class Neo4jGrailsPlugin {

    def license = "WTFPL"
    def organization = [ name: "Stefan Armbruster", url: "http://www.armbruster-it.de/" ]
    def developers = [
        [ name: "Stefan Armbruster", email: "stefan@armbruster-it.de" ] ]
    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPNEO4J" ]
    def scm = [ url: "https://github.com/sarmbruster/spring-data-mapping" ]

    def version = "0.9"
    def grailsVersion = "1.3.5 > *"
    //def observe = ['services']
    //def loadAfter = ['domainClass', 'hibernate', 'services', 'cloudFoundry']
    def loadAfter = ['domainClass', 'hibernate', 'services', 'cloudFoundry']
    def author = "Stefan Armbruster"
    def authorEmail = "stefan@armbruster-it.de"
    def title = "Neo4j GORM"
    def description = '''\\
A plugin that integrates the Neo4j graph database into Grails, providing
a GORM API onto it
'''

    def documentation = "http://grails.org/plugin/neo4j"

    def dependsOn = [:]
    // resources that are excluded from plugin packaging

    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]


	def defaultStoreDir = "data/neo4j"
	def defaultStoreClass = EmbeddedGraphDatabase
	def defaultConfig = [:]

    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional), this event occurs before 
    }

    def doWithSpring = {
        neo4jTransactionManager(DatastoreTransactionManager) {
            datastore = ref("neo4jDatastore")
        }

        neo4jMappingContext(Neo4jMappingContextFactoryBean) {
            grailsApplication = ref('grailsApplication')
            pluginManager = ref('pluginManager')
        }

	    graphDatabaseService(
			     application.config.grails.neo4j.storeClass ?: defaultStoreClass,
			     application.config.grails.neo4j.storeDir ?: defaultStoreDir,
			     application.config.grails.neo4j.config ?: defaultConfig

	    ) { bean ->
            bean.destroyMethod = "shutdown"
        }

/*
        indexService(LuceneFulltextQueryIndexService, ref("graphDatabaseService")) { bean ->
        //indexService(LuceneFulltextIndexService, ref("graphDatabaseService")) { bean ->
            bean.destroyMethod = "shutdown"
        }
*/

        neo4jDatastore(Neo4jDatastoreFactoryBean) {
            graphDatabaseService = graphDatabaseService
            mappingContext = neo4jMappingContext

        }

        neo4jPersistenceInterceptor(DatastorePersistenceContextInterceptor, ref("neo4jDatastore"))

        //neo4jPersistenceContextInterceptorAggregator(PersistenceContextInterceptorAggregato)


        if (manager?.hasGrailsPlugin("controllers")) {
            neo4jOpenSessionInViewInterceptor(Neo4jOpenSessionInViewInterceptor) {
                datastore = ref("neo4jDatastore")
            }
            if (getSpringConfig().containsBean("controllerHandlerMappings")) {
                controllerHandlerMappings.interceptors << neo4jOpenSessionInViewInterceptor
            }
            if (getSpringConfig().containsBean("annotationHandlerMapping")) {
                if (annotationHandlerMapping.interceptors) {
                    annotationHandlerMapping.interceptors << neo4jOpenSessionInViewInterceptor
                }
                else {
                    annotationHandlerMapping.interceptors = [neo4jOpenSessionInViewInterceptor]
                }
            }
        }

        // need to fix the service proxies to use mongoTransactionManager
        for (serviceGrailsClass in application.serviceClasses) {
            GrailsServiceClass serviceClass = serviceGrailsClass

            if (!shouldCreateTransactionalProxy(serviceClass)) {
                continue
            }

            def beanName = serviceClass.propertyName
            if (springConfig.containsBean(beanName)) {
                delegate."$beanName".transactionManager = ref('neo4jTransactionManager')
            }
        }

        // make sure validators for Mongo domain classes are regular GrailsDomainClassValidator
        def isHibernateInstalled = manager.hasGrailsPlugin("hibernate")
        for (dc in application.domainClasses) {
            def cls = dc.clazz
            def cpf = ClassPropertyFetcher.forClass(cls)
            def mappedWith = cpf.getStaticPropertyValue(GrailsDomainClassProperty.MAPPING_STRATEGY, String)
            if (mappedWith == 'neo4j' || (!isHibernateInstalled && mappedWith == null)) {
                String validatorBeanName = "${dc.fullName}Validator"
                def beandef = springConfig.getBeanConfig(validatorBeanName)?.beanDefinition ?:
                              springConfig.getBeanDefinition(validatorBeanName)
                beandef.beanClassName = GrailsDomainClassValidator.name
            }
        }

    }

    def doWithDynamicMethods = { ctx ->
        Datastore datastore = ctx.neo4jDatastore
        PlatformTransactionManager transactionManager = null // ctx.mongoTransactionManager
        def enhancer = transactionManager ?
            new Neo4jGormEnhancer(datastore, transactionManager) :
            new Neo4jGormEnhancer(datastore)

        def isHibernateInstalled = manager.hasGrailsPlugin("hibernate")
        for (entity in datastore.mappingContext.persistentEntities) {
            def cls = entity.javaClass
            def cpf = ClassPropertyFetcher.forClass(cls)
            def mappedWith = cpf.getStaticPropertyValue(GrailsDomainClassProperty.MAPPING_STRATEGY, String)
            if (isHibernateInstalled) {
                if (mappedWith == 'neo4j') {
                    enhancer.enhance(entity)
                }
                else {
                    def staticApi = new GormStaticApi(cls, datastore)
                    def instanceApi = new GormInstanceApi(cls, datastore)
                    cls.metaClass.static.getNeo4j = {-> staticApi }
                    cls.metaClass.getNeo4j = {-> new InstanceProxy(instance:delegate, target:instanceApi) }
                }
            }
            else {
                if (mappedWith == 'neo4j' || mappedWith == null) {
                    enhancer.enhance(entity)
                }
            }
        }
    }

    def doWithApplicationContext = { applicationContext ->
        // TODO Implement post initialization spring config (optional)
    }

    def onChange = { event ->
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }
}
