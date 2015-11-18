/* Copyright (C) 2014 SpringSource
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.orm.bootstrap
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Commons
import org.grails.datastore.gorm.bootstrap.AbstractDatastoreInitializer
import org.grails.datastore.gorm.proxy.ProxyHandlerAdapter
import org.grails.datastore.gorm.support.AbstractDatastorePersistenceContextInterceptor
import org.grails.datastore.gorm.support.DatastorePersistenceContextInterceptor
import org.grails.datastore.gorm.validation.CascadingValidator
import org.grails.datastore.mapping.model.DatastoreConfigurationException
import org.grails.orm.hibernate.*
import org.grails.orm.hibernate.cfg.GrailsDomainBinder
import org.grails.orm.hibernate.cfg.HibernateUtils
import org.grails.orm.hibernate.cfg.Mapping
import org.grails.orm.hibernate.proxy.HibernateProxyHandler
import org.grails.orm.hibernate.support.*
import org.grails.orm.hibernate.validation.HibernateDomainClassValidator
import org.grails.orm.hibernate3.support.AggregatePersistenceContextInterceptor
import org.grails.orm.hibernate3.support.GrailsOpenSessionInViewInterceptor
import org.hibernate.EmptyInterceptor
import org.hibernate.cfg.ImprovedNamingStrategy
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.PropertiesFactoryBean
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.support.GenericApplicationContext

import javax.sql.DataSource
/**
 * Class that handles the details of initializing GORM for Hibernate
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@Commons
class HibernateDatastoreSpringInitializer extends AbstractDatastoreInitializer {
    public static final String SESSION_FACTORY_BEAN_NAME = "sessionFactory"
    public static final String DEFAULT_DATA_SOURCE_NAME = 'dataSource'

    String defaultDataSourceBeanName = Mapping.DEFAULT_DATA_SOURCE
    String defaultSessionFactoryBeanName = SESSION_FACTORY_BEAN_NAME
    String ddlAuto = "update"
    Set<String> dataSources = [defaultDataSourceBeanName]

    HibernateDatastoreSpringInitializer() {
    }

    HibernateDatastoreSpringInitializer(ClassLoader classLoader, String... packages) {
        super(classLoader, packages)
    }
    HibernateDatastoreSpringInitializer(String... packages) {
        super(packages)
    }

    HibernateDatastoreSpringInitializer(Collection<Class> persistentClasses) {
        super(persistentClasses)
    }

    HibernateDatastoreSpringInitializer(Class... persistentClasses) {
        super(persistentClasses.toList())
    }

    HibernateDatastoreSpringInitializer(Map hibernateProperties, Collection<Class> persistentClasses) {
        super(hibernateProperties, persistentClasses)
    }

    HibernateDatastoreSpringInitializer(Map hibernateProperties, Class... persistentClasses) {
        super(hibernateProperties, persistentClasses.toList())
    }

    @Override
    protected Class<AbstractDatastorePersistenceContextInterceptor> getPersistenceInterceptorClass() {
        DatastorePersistenceContextInterceptor
    }

    @CompileStatic
    ApplicationContext configureForDataSource(DataSource dataSource) {
        GenericApplicationContext applicationContext = new GenericApplicationContext()
        applicationContext.beanFactory.registerSingleton(DEFAULT_DATA_SOURCE_NAME, dataSource)
        configureForBeanDefinitionRegistry(applicationContext)
        applicationContext.refresh()
        return applicationContext
    }

    @CompileStatic
    ApplicationContext configureForDataSources(Map<String, DataSource> dataSources) {
        GenericApplicationContext applicationContext = new GenericApplicationContext()
        boolean hasDefault = false
        for(name in dataSources.keySet()) {
            def ds = dataSources.get(name)
            if(name == DEFAULT_DATA_SOURCE_NAME) {
                hasDefault = true
                applicationContext.beanFactory.registerSingleton(name, ds)
            }
            else {
                this.dataSources.add(name)
                applicationContext.beanFactory.registerSingleton("dataSource_$name", ds)
            }
        }

        if(!hasDefault) {
            throw new DatastoreConfigurationException("At least one DataSource must be named 'dataSource' to act as the default DataSource")
        }
        configureForBeanDefinitionRegistry(applicationContext)
        applicationContext.refresh()
        return applicationContext
    }


    public Closure getBeanDefinitions(BeanDefinitionRegistry beanDefinitionRegistry) {
        Closure beanDefinitions = {
            def common = getCommonConfiguration(beanDefinitionRegistry)
            common.delegate = delegate
            common.call()

            Object vendorToDialect = getVenderToDialectMappings()

            // for unwrapping / inspecting proxies
            hibernateProxyHandler(HibernateProxyHandler)
            proxyHandler(ProxyHandlerAdapter, ref('hibernateProxyHandler'))

            // for handling GORM events
            eventTriggeringInterceptor(ClosureEventTriggeringInterceptor)
            // for listening to Hibernate events
            hibernateEventListeners(HibernateEventListeners)
            // Useful interceptor for wrapping Hibernate behavior
            persistenceInterceptor(AggregatePersistenceContextInterceptor)
            // domain model mapping context, used for configuration
            grailsDomainClassMappingContext(HibernateMappingContextFactoryBean) {
                delegate.configuration = this.configuration
                proxyFactory = hibernateProxyHandler
                delegate.persistentClasses = persistentClasses
            }


            def config = this.configuration
            for(dataSourceName in dataSources) {

                boolean isDefault = dataSourceName == defaultDataSourceBeanName
                String suffix = isDefault ? '' : '_' + dataSourceName
                String prefix = isDefault ? '' : dataSourceName + '_'
                def sessionFactoryName = isDefault ? defaultSessionFactoryBeanName : "sessionFactory$suffix"

                def hibConfig = config.getProperty("hibernate$suffix", Map, Collections.emptyMap())
                def dsConfigPrefix = config.containsProperty('dataSources') ? "dataSources.${isDefault ? DEFAULT_DATA_SOURCE_NAME : dataSourceName}" : DEFAULT_DATA_SOURCE_NAME
                def ddlAutoSetting = config.getProperty("${dsConfigPrefix}.dbCreate", ddlAuto)

                // default interceptor, can be overridden for extensibility
                def entityInterceptorName = "entityInterceptor$suffix"
                if(!beanDefinitionRegistry.containsBeanDefinition(entityInterceptorName)) {
                    "$entityInterceptorName"(EmptyInterceptor)
                }

                def hibernateProperties = new Properties()
                if(hibConfig) {
                    def subProps = hibConfig.respondsTo('toProperties') ? hibConfig.toProperties() : hibConfig
                    for(key in subProps.keySet()) {
                        hibernateProperties["hibernate.${key}".toString()] = subProps.get(key)
                    }
                }

                if (!hibernateProperties['hibernate.hbm2ddl.auto']) {
                    hibernateProperties['hibernate.hbm2ddl.auto'] = ddlAutoSetting
                }

                def noDialect = !hibernateProperties['hibernate.dialect']
                def hibConfigClass = config.getProperty("${dsConfigPrefix}.configClass", (String)hibernateProperties['hibernate.config_class']?.toString())
                Properties noDialectProperties = new Properties(hibernateProperties)
                if (noDialect) {
                    hibernateProperties['hibernate.dialect'] = ref("dialectDetector")
                }
                "hibernateProperties$suffix"(PropertiesFactoryBean) { bean ->
                    bean.scope = "prototype"
                    properties = hibernateProperties
                }

                // override Validator beans with Hibernate aware instances
                for(cls in persistentClasses) {
                    "${cls.name}Validator$suffix"(HibernateDomainClassValidator) {
                        messageSource = ref("messageSource")
                        domainClass = ref("${cls.name}DomainClass")
                        grailsApplication = ref('grailsApplication')
                        hibernateDatastore = ref("hibernateDatastore$suffix")
                    }
                }
                // Used to detect the database dialect to use
                if(noDialect) {
                    "dialectDetector$suffix"(HibernateDialectDetectorFactoryBean) {
                        dataSource = ref("dataSource$suffix")
                        vendorNameDialectMappings = vendorToDialect
                        delegate.hibernateProperties = noDialectProperties
                    }
                }

                def namingStrategy = hibConfig.naming_strategy ?: ImprovedNamingStrategy
                try {
                    GrailsDomainBinder.configureNamingStrategy dataSourceName, namingStrategy
                }
                catch (Throwable t) {
                    log.error """WARNING: You've configured a custom Hibernate naming strategy '$namingStrategy' in DataSource.groovy, however the class cannot be found.
Using Grails' default naming strategy: '${ImprovedNamingStrategy.name}'"""
                    GrailsDomainBinder.configureNamingStrategy dataSourceName, ImprovedNamingStrategy
                }


                // the main SessionFactory bean
                if(!beanDefinitionRegistry.containsBeanDefinition(sessionFactoryName)) {
                    "$sessionFactoryName"(HibernateMappingContextSessionFactoryBean) { bean ->
                        delegate.dataSourceName = dataSourceName
                        dataSource = ref("dataSource$suffix")
                        delegate.hibernateMappingContext = ref('grailsDomainClassMappingContext')
                        delegate.hibernateProperties = ref("hibernateProperties$suffix")
                        entityInterceptor = ref("entityInterceptor$suffix")
                        sessionFactoryBeanName = sessionFactoryName

                        List hibConfigLocations = []
                        def cl = this.classLoader ?: Thread.currentThread().contextClassLoader
                        if (cl.getResource(prefix + 'hibernate.cfg.xml')) {
                            hibConfigLocations << 'classpath:' + prefix + 'hibernate.cfg.xml'
                        }
                        configLocations = hibConfigLocations
                        if(hibConfigClass) {
                            configClass = hibConfigClass
                        }
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
                }


                "hibernateDatastore$suffix"(HibernateDatastoreFactoryBean, HibernateDatastore, ref('grailsDomainClassMappingContext'), ref(sessionFactoryName), configuration, dataSourceName)

                if (!beanDefinitionRegistry.containsBeanDefinition("transactionManager$suffix")) {
                    "transactionManager$suffix"(GrailsHibernateTransactionManager) { bean ->
                        bean.autowire = "byName"
                        sessionFactory = ref(sessionFactoryName)
                        dataSource = ref("dataSource$suffix")
                    }
                }

                "org.grails.gorm.hibernate.internal.POST_INIT_BEAN-${dataSourceName}$suffix"(PostInitializationHandling) { bean ->
                    grailsApplication = ref('grailsApplication')
                    bean.lazyInit = false
                }

                boolean osivEnabled = config.getProperty("hibernate${suffix}.osiv.enabled", Boolean, true)
                boolean isWebApplication = beanDefinitionRegistry?.containsBeanDefinition("dispatcherServlet") ||
                                                beanDefinitionRegistry?.containsBeanDefinition("grailsControllerHelper")
                if (isWebApplication && osivEnabled) {
                    "flushingRedirectEventListener$suffix"(FlushOnRedirectEventListener, ref("hibernateDatastore$suffix"))

                    "openSessionInViewInterceptor$suffix"(GrailsOpenSessionInViewInterceptor) {
                        flushMode = HibernateDatastoreSpringInitializer.resolveDefaultFlushMode(config.getProperty("hibernate${suffix}.flush.mode"),
                                                                                                    config.getProperty("hibernate${suffix}.osiv.readonly", Boolean, false))
                        sessionFactory = ref(sessionFactoryName)
                    }
                }
            }


        }
        beanDefinitions
    }


    @CompileStatic
    private static int resolveDefaultFlushMode(CharSequence flushModeStr, boolean readOnly) {
        int flushMode
        if (Boolean.TRUE.equals(readOnly)) {
            flushMode = GrailsHibernateTemplate.FLUSH_NEVER
        }
        else if (flushModeStr instanceof CharSequence) {
            switch(flushModeStr.toString().toLowerCase()) {
                case "manual":
                case "never":
                    flushMode = GrailsHibernateTemplate.FLUSH_NEVER
                    break
                case "always":
                    flushMode = GrailsHibernateTemplate.FLUSH_ALWAYS
                    break
                case "commit":
                    flushMode = GrailsHibernateTemplate.FLUSH_COMMIT
                    break
                default:
                    flushMode = GrailsHibernateTemplate.FLUSH_AUTO
            }
        }
        else {
            flushMode = GrailsHibernateTemplate.FLUSH_AUTO
        }
        return flushMode
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
    static class PostInitializationHandling implements InitializingBean, ApplicationContextAware {

        @Autowired
        def grailsApplication

        // required to force initialisation of validators
        @Autowired(required = false)
        CascadingValidator[] validators = []

        ApplicationContext applicationContext

        @Override
        @CompileDynamic
        void afterPropertiesSet() throws Exception {
            grailsApplication.setMainContext(applicationContext)
            HibernateUtils.enhanceSessionFactories(applicationContext)
        }
    }

}
