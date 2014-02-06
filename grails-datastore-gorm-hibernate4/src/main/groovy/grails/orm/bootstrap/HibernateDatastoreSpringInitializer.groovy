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
import org.springframework.context.support.GenericApplicationContext
import org.springframework.jdbc.support.nativejdbc.CommonsDbcpNativeJdbcExtractor

import javax.sql.DataSource

/**
 * Class that handles the details of initializing GORM for Hibernate
 *
 * @author Graeme Rocher
 * @since 3.0
 */
class HibernateDatastoreSpringInitializer {
    public static final String SESSION_FACTORY_BEAN_NAME = "sessionFactory"
    Properties hibernateProperties = new Properties()
    Collection<Class> persistentClasses

    private String dataSourceBeanName = "dataSource"
    private String sessionFactoryBeanName = "sessionFactory"

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

    private setDataSourceBeanName(String dataSourceBeanName) {
        this.dataSourceBeanName = dataSourceBeanName
    }

    @CompileStatic
    ApplicationContext configureForDataSource(DataSource dataSource) {
        ExpandoMetaClass.enableGlobally()
        GenericApplicationContext applicationContext = new GenericApplicationContext()
        applicationContext.beanFactory.registerSingleton(dataSourceBeanName, dataSource)
        configureForBeanDefinitionRegistry(applicationContext)
        applicationContext.refresh()
        return applicationContext
    }

    @CompileStatic
    void configureForBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) {
        ExpandoMetaClass.enableGlobally()

        if( GroovyBeanReaderInit.isAvailable() ) {
            GroovyBeanReaderInit.registerBeans(beanDefinitionRegistry, getBeanDefinitions(beanDefinitionRegistry))
        }
        else if (GrailsBeanBuilderInit.isAvailable() ) {
            GrailsBeanBuilderInit.registerBeans(beanDefinitionRegistry, getBeanDefinitions(beanDefinitionRegistry))
        }
        else {
            throw new IllegalStateException("Neither Spring 4.0 nor grails-spring dependency found on classpath to enable GORM configuration. If you are using an earlier version of Spring please add the grails-spring dependency to your classpath.")
        }
    }

    public Closure getBeanDefinitions(BeanDefinitionRegistry beanDefinitionRegistry) {
        Closure beanDefinitions = {
            xmlns context: "http://www.springframework.org/schema/context"
            context.'annotation-config'()

            Object vendorToDialect = getVenderToDialectMappings()
            "dialectDetector"(HibernateDialectDetectorFactoryBean) {
                dataSource = ref(dataSourceBeanName)
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
            } else {
                // TODO: add bean to register for pre-existing GrailsApplication
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
                    sessionFactory = ref(sessionFactoryBeanName)
                }
            }
            eventTriggeringInterceptor(ClosureEventTriggeringInterceptor)
            nativeJdbcExtractor(CommonsDbcpNativeJdbcExtractor)
            hibernateEventListeners(HibernateEventListeners)
            entityInterceptor(EmptyInterceptor)
            sessionFactory(ConfigurableLocalSessionFactoryBean) { bean ->
                bean.autowire = "byType"
                dataSource = ref(dataSourceBeanName)
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
            hibernateDatastore(HibernateDatastore, ref('grailsDomainClassMappingContext'), ref(sessionFactoryBeanName), ref('grailsHibernateConfig'))

            if (!beanDefinitionRegistry.containsBeanDefinition("transactionManager")) {
                transactionManager(GrailsHibernateTransactionManager) { bean ->
                    bean.autowire = "byName"
                    sessionFactory = ref(sessionFactoryBeanName)
                    dataSource = ref(dataSourceBeanName)
                }
            }

            "org.grails.gorm.hibernate.internal.GORM_ENHANCER_BEAN-${dataSourceBeanName}"(HibernateGormEnhancer, ref("hibernateDatastore"), ref("transactionManager"), ref("grailsApplication")) { bean ->
                bean.initMethod = 'enhance'
                bean.lazyInit = false
            }

            "org.grails.gorm.hibernate.internal.POST_INIT_BEAN-${dataSourceBeanName}"(PostInializationHandling) { bean ->
                grailsApplication = ref(GrailsApplication.APPLICATION_ID)
                bean.lazyInit = false
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

    static class GroovyBeanReaderInit {
        static boolean isAvailable() {
            try {
                Thread.currentThread().contextClassLoader.loadClass('org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader')
                return true
            } catch (e) {
                return false
            }
        }
        static void registerBeans(BeanDefinitionRegistry registry, Closure beanDefinitions) {
            def classLoader = Thread.currentThread().contextClassLoader
            def beanReader = classLoader.loadClass('org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader').newInstance(registry)
            beanReader.beans beanDefinitions
        }
    }
    static class GrailsBeanBuilderInit {
        static boolean isAvailable() {
            try {
                Thread.currentThread().contextClassLoader.loadClass('grails.spring.BeanBuilder')
                return true
            } catch (e) {
                return false
            }
        }

        static void registerBeans(BeanDefinitionRegistry registry, Closure beanDefinitions) {
            def classLoader = Thread.currentThread().contextClassLoader
            def beanBuilder = classLoader.loadClass('grails.spring.BeanBuilder').newInstance()
            beanBuilder.beans beanDefinitions
            beanBuilder.registerBeans registry
        }
    }
}
