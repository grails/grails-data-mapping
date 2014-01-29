package grails.orm.bootstrap

import grails.spring.BeanBuilder
import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.orm.hibernate.*
import org.codehaus.groovy.grails.orm.hibernate.support.ClosureEventTriggeringInterceptor
import org.codehaus.groovy.grails.orm.hibernate.support.HibernateDialectDetectorFactoryBean
import org.codehaus.groovy.grails.orm.hibernate.validation.HibernateDomainClassValidator
import org.grails.datastore.gorm.config.GrailsDomainClassMappingContext
import org.hibernate.EmptyInterceptor
import org.hibernate.SessionFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.MethodInvokingFactoryBean
import org.springframework.beans.factory.config.PropertiesFactoryBean
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.jdbc.support.nativejdbc.CommonsDbcpNativeJdbcExtractor

/**
 * Class that handles the details of initializing GORM for Hibernate
 *
 * @author Graeme Rocher
 * @since 3.0
 */
class HibernateDatastoreSpringInitializer
{

    public static final String SESSION_FACTORY_BEAN_NAME = "sessionFactory"
    public static final String DATA_SOURCE_BEAN_NAME = "dataSource"
    Properties hibernateProperties = new Properties()
    Collection<Class> persistentClasses

    HibernateDatastoreSpringInitializer(Collection<Class> persistentClasses) {
        this.persistentClasses = persistentClasses
    }

    HibernateDatastoreSpringInitializer(Class... persistentClasses) {
        this(persistentClasses.toList())
    }

    HibernateDatastoreSpringInitializer(Properties hibernateProperties, Collection<Class> persistentClasses) {
        this.hibernateProperties = hibernateProperties
        this.persistentClasses = persistentClasses
    }

    HibernateDatastoreSpringInitializer(Properties hibernateProperties, Class... persistentClasses) {
        this(hibernateProperties, persistentClasses.toList())
    }

    void configure(BeanDefinitionRegistry beanDefinitionRegistry) {
        def beanBuilder = new BeanBuilder()
        Closure beanDefinitions = getBeanDefinitions(beanDefinitionRegistry)
        beanBuilder.beans beanDefinitions
        beanBuilder.registerBeans(beanDefinitionRegistry)
    }

    public Closure getBeanDefinitions(BeanDefinitionRegistry beanDefinitionRegistry) {
        Closure beanDefinitions = {
            Object vendorToDialect = getVenderToDialectMappings()
            "dialectDetector"(HibernateDialectDetectorFactoryBean) {
                dataSource = ref("dataSource")
                vendorNameDialectMappings = vendorToDialect
            }

            if (!hibernateProperties['hibernate.hbm2ddl.auto']) {
                hibernateProperties['hibernate.hbm2ddl.auto'] = 'update'
            }
            if (!hibernateProperties['hibernate.dialect']) {
                hibernateProperties['hibernate.dialect'] = ref("dialectDetector")
            }


            "hibernateProperties"(PropertiesFactoryBean) { bean ->
                bean.scope = "prototype"
                properties = this.hibernateProperties
            }

            if (!beanDefinitionRegistry.containsBeanDefinition(GrailsApplication.APPLICATION_ID)) {
                grailsApplication(DefaultGrailsApplication, persistentClasses as Class[], Thread.currentThread().contextClassLoader) { bean ->
                    bean.initMethod = 'initialise'
                }
            }

            for (Class dc in persistentClasses) {
                "${dc.name}"(dc) { bean ->
                    bean.singleton = false
                    bean.autowire = "byName"
                }
                "${dc.name}DomainClass"(MethodInvokingFactoryBean) { bean ->
                    targetObject = ref(GrailsApplication.APPLICATION_ID)
                    targetMethod = "getArtefact"
                    bean.lazyInit = true
                    arguments = [DomainClassArtefactHandler.TYPE, dc.name]
                }
                "${dc.name}Validator"(HibernateDomainClassValidator) {
                    domainClass = ref("${dc.name}DomainClass")
                    grailsApplication = ref(GrailsApplication.APPLICATION_ID)
                    sessionFactory = ref(SESSION_FACTORY_BEAN_NAME)
                }
            }
            eventTriggeringInterceptor(ClosureEventTriggeringInterceptor)
            nativeJdbcExtractor(CommonsDbcpNativeJdbcExtractor)
            hibernateEventListeners(HibernateEventListeners)
            entityInterceptor(EmptyInterceptor)
            sessionFactory(ConfigurableLocalSessionFactoryBean) { bean ->
                bean.autowire = "byType"
                dataSource = ref(DATA_SOURCE_BEAN_NAME)
                delegate.hibernateProperties = ref('hibernateProperties')
                grailsApplication = ref(GrailsApplication.APPLICATION_ID)
                eventListeners = [
                        'save': eventTriggeringInterceptor,
                        'save-update': eventTriggeringInterceptor,
                        'pre-load': eventTriggeringInterceptor,
                        'post-load': eventTriggeringInterceptor,
                        'pre-insert': eventTriggeringInterceptor,
                        'post-insert': eventTriggeringInterceptor,
                        'pre-update': eventTriggeringInterceptor,
                        'post-update': eventTriggeringInterceptor,
                        'pre-delete': eventTriggeringInterceptor,
                        'post-delete': eventTriggeringInterceptor]
                hibernateEventListeners = ref('hibernateEventListeners')

            }

            grailsHibernateConfig(MethodInvokingFactoryBean) { bean ->
                targetObject = ref(GrailsApplication.APPLICATION_ID)
                targetMethod = "getConfig"
            }
            grailsDomainClassMappingContext(GrailsDomainClassMappingContext, ref(GrailsApplication.APPLICATION_ID))
            hibernateDatastore(HibernateDatastore, ref('grailsDomainClassMappingContext'), ref(SESSION_FACTORY_BEAN_NAME), ref('grailsHibernateConfig'))

            if (!beanDefinitionRegistry.containsBeanDefinition("transactionManager")) {
                transactionManager(GrailsHibernateTransactionManager) {
                    sessionFactory = ref(SESSION_FACTORY_BEAN_NAME)
                    dataSource = ref(DATA_SOURCE_BEAN_NAME)
                }
            }

            hibernateGormEnhancer(HibernateGormEnhancer, ref("hibernateDatastore"), ref("transactionManager"), ref("grailsApplication")) { bean ->
                bean.initMethod = 'enhance'
            }

            "org.grails.gorm.hibernate.internal.POST_INIT_BEAN"(PostInializationHandling) {
                grailsApplication = ref(GrailsApplication.APPLICATION_ID)
            }
        }
        beanDefinitions
    }

    protected Properties getVenderToDialectMappings() {
        def vendorToDialect = new Properties()
        def hibernateDialects = Thread.currentThread().contextClassLoader.getResource("hibernate-dialects.properties")
        if (hibernateDialects) {
            def p = new Properties()
            hibernateDialects.withInputStream { InputStream input ->
                p.load(input)
            }

            for (entry in p) {
                vendorToDialect[entry.value] = "org.hibernate.dialect.${entry.key}".toString()
            }
        }
        vendorToDialect
    }

    @CompileStatic
    static class PostInializationHandling implements InitializingBean, ApplicationContextAware {

        @Autowired
        GrailsApplication grailsApplication

        ApplicationContext applicationContext

        @Override
        void afterPropertiesSet() throws Exception {
            grailsApplication.setMainContext(applicationContext)

            def hibernateDatastores = applicationContext.getBeansOfType(HibernateDatastore)
            def datastoreMap = new HashMap<SessionFactory, HibernateDatastore>()
            for (HibernateDatastore hibernateDatastore : hibernateDatastores.values()) {
                datastoreMap[hibernateDatastore.sessionFactory] = hibernateDatastore
            }
            applicationContext.getBean(ClosureEventTriggeringInterceptor).datastores = datastoreMap
        }
    }
}
