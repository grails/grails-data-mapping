/*
 * Copyright 2014 the original author or authors.
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

package grails.test.runtime.gorm

import grails.core.GrailsApplication
import grails.orm.bootstrap.HibernateDatastoreSpringInitializer
import grails.persistence.support.PersistenceContextInterceptor
import grails.test.runtime.TestEvent
import grails.test.runtime.TestPlugin
import grails.test.runtime.TestRuntime
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.apache.tomcat.jdbc.pool.DataSource as TomcatDataSource
import org.grails.core.artefact.DomainClassArtefactHandler
import org.grails.orm.hibernate.support.AbstractMultipleDataSourceAggregatePersistenceContextInterceptor
import org.grails.spring.beans.factory.InstanceFactoryBean
import org.grails.test.support.GrailsTestTransactionInterceptor
import org.hibernate.SessionFactory
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.core.env.PropertyResolver
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy

import javax.sql.DataSource
/**
 * a TestPlugin for TestRuntime for adding Grails DomainClass (GORM) support
 * 
 * @author Lari Hotari
 * @since 2.4.1
 *
 */
@CompileStatic
class HibernateTestPlugin implements TestPlugin {
    String[] requiredFeatures = ['grailsApplication', 'coreBeans']
    String[] providedFeatures = ['domainClass', 'gorm', 'hibernateGorm']
    int ordinal = 1
    static Properties defaultHibernateConfig = ['hibernate.cache.use_second_level_cache': 'true', 'hibernate.cache.use_query_cache': 'false', 'hibernate.cache.region.factory_class': 'org.hibernate.cache.ehcache.EhCacheRegionFactory'] as Properties 
    
    void connectPersistenceInterceptor(TestRuntime runtime) {
        GrailsApplication grailsApplication = getGrailsApplication(runtime)

        def mainContext = grailsApplication.getMainContext()
        if(mainContext.containsBean("persistenceInterceptor")) {
            def persistenceInterceptor = mainContext.getBean("persistenceInterceptor", PersistenceContextInterceptor)
            persistenceInterceptor.init()
            runtime.putValue("hibernateInterceptor", persistenceInterceptor)
        }
        // force init
        mainContext.getBeansOfType(HibernateDatastoreSpringInitializer.PostInitializationHandling)
    }

    void destroyPersistenceInterceptor(TestRuntime runtime) {
        if(runtime.containsValueFor("hibernateInterceptor")) {
            PersistenceContextInterceptor persistenceInterceptor=(PersistenceContextInterceptor)runtime.removeValue("hibernateInterceptor")
            persistenceInterceptor.destroy()
        }
    }
    
    void cleanupSessionFactory(TestRuntime runtime) {
        GrailsApplication grailsApplication = getGrailsApplication(runtime)
        if(grailsApplication.getMainContext().containsBean("sessionFactory")) {
            SessionFactory sessionFactory = grailsApplication.getMainContext().getBean("sessionFactory", SessionFactory)
            if(sessionFactory != null && !sessionFactory.isClosed()) {
                try {
                    sessionFactory.close()
                } catch (Exception e) {
                    // ignore exception
                }
            }
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected void configureDefaultDataSources(TestRuntime runtime, grails.core.GrailsApplication application, boolean immediateDelivery = true) {


        def dataSourceNames = AbstractMultipleDataSourceAggregatePersistenceContextInterceptor.calculateDataSourceNames(application)
        dataSourceNames = dataSourceNames ?: ['DEFAULT']


        defineBeans(runtime, immediateDelivery) {
            for(ds in dataSourceNames){
                def suffix = ds == 'DEFAULT' ? '' : "_$ds"
                "dataSourceUnproxied${suffix}"(TomcatDataSource) { bean ->
                    bean.destroyMethod = "close"
                    url = "jdbc:h2:mem:grailsDB${suffix};MVCC=TRUE;LOCK_TIMEOUT=10000"
                    driverClassName = "org.h2.Driver"
                    username = "sa"
                    password = ""
                    initialSize = 1
                    minIdle = 1
                    maxIdle = 1
                    maxActive = 10
                    maxWait = 10000
                    maxAge = 10 * 60000
                    timeBetweenEvictionRunsMillis = 5000
                    minEvictableIdleTimeMillis = 60000
                    validationQuery = "SELECT 1"
                    validationQueryTimeout = 3
                    validationInterval = 15000
                    testOnBorrow = true
                    testWhileIdle = true
                    testOnReturn = false
                    jdbcInterceptors = "ConnectionState"
                    defaultTransactionIsolation = java.sql.Connection.TRANSACTION_READ_COMMITTED
                }
                "dataSourceLazy${suffix}"(LazyConnectionDataSourceProxy, ref('dataSourceUnproxied'))
                "dataSource${suffix}"(TransactionAwareDataSourceProxy, ref('dataSourceLazy'))

            }
        }
    }

    /**
     * Sets up a GORM for Hibernate domain
     *
     * @param persistentClasses
     */
    void hibernateDomain(TestRuntime runtime, Map parameters) {
        Collection<Class> persistentClasses = [] as Set
        persistentClasses.addAll((Collection<Class>)parameters.domains)
        boolean immediateDelivery = true
        if(runtime.containsValueFor("hibernatePersistentClassesToRegister")) {
            Collection<Class<?>> allPersistentClasses = runtime.getValue("hibernatePersistentClassesToRegister", Collection)
            allPersistentClasses.addAll(persistentClasses)
            immediateDelivery = false
        }

        DataSource dataSource = (DataSource)parameters.dataSource
        if(dataSource != null) {
            defineDataSourceBean(runtime, immediateDelivery, dataSource)
        }

        Map initializerConfig

        def configArgument = parameters.config
        if(configArgument instanceof Map) {
            initializerConfig = (Map)configArgument
        }
        
        if(immediateDelivery) {
            Collection<Class<?>> previousPersistentClasses = runtime.getValue("initializedHibernatePersistentClasses", Collection)
            if(!previousPersistentClasses?.containsAll(persistentClasses) || initializerConfig || dataSource) {
                if(previousPersistentClasses) {
                    persistentClasses.addAll(previousPersistentClasses)
                }
                boolean reconnectPersistenceInterceptor = false
                if(runtime.containsValueFor("hibernateInterceptor")) {
                    destroyPersistenceInterceptor(runtime)
                    cleanupSessionFactory(runtime)
                    reconnectPersistenceInterceptor = true
                }
                registerHibernateDomains(runtime, runtime.getValueIfExists("grailsApplication", GrailsApplication), persistentClasses, initializerConfig, true)
                if(reconnectPersistenceInterceptor) {
                    connectPersistenceInterceptor(runtime)
                }
            }
        } else {
            if(initializerConfig) {
                runtime.putValue("hibernateInitializerConfig", initializerConfig)
            }
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private defineDataSourceBean(TestRuntime runtime, boolean immediateDelivery, DataSource dataSource) {
        defineBeans(runtime, immediateDelivery) {
            delegate.dataSource(InstanceFactoryBean, dataSource)
        }
    }

    protected void before(TestRuntime runtime, Object target) {
        connectPersistenceInterceptor(runtime)
        initTransaction(runtime, target)
    }

    protected void after(TestRuntime runtime) {
        destroyTransaction(runtime)
        destroyPersistenceInterceptor(runtime)
    }
    
    void defineBeans(TestRuntime runtime, boolean immediateDelivery = true, Closure<?> closure) {
        runtime.publishEvent("defineBeans", [closure: closure], [immediateDelivery: immediateDelivery])
    }
    
    GrailsApplication getGrailsApplication(TestRuntime runtime) {
        runtime.getValue("grailsApplication", GrailsApplication)
    }
    
    void registerBeans(TestRuntime runtime, GrailsApplication grailsApplication) {
        // workaround for GRAILS-11456
        if(!grailsApplication.config.containsKey("dataSource")) {
            grailsApplication.config.getProperty("dataSource")
        }
        def config = grailsApplication.config
        config.merge((Map)defaultHibernateConfig)
        config.merge((Map)runtime.getValueIfExists("hibernateInitializerConfig", Map))

        configureDefaultDataSources(runtime, grailsApplication, false)
        if(runtime.containsValueFor("hibernatePersistentClassesToRegister")) {
            Collection<Class<?>> persistentClasses = runtime.getValue("hibernatePersistentClassesToRegister", Collection)
            registerHibernateDomains(runtime, grailsApplication, persistentClasses, runtime.getValueIfExists("hibernateInitializerConfig", Map), false)
        }
    }

    void registerHibernateDomains(TestRuntime runtime, GrailsApplication grailsApplication, Collection<Class<?>> persistentClasses, Map initializerConfig, boolean immediateDelivery) {
        for(cls in persistentClasses) {
            grailsApplication.addArtefact(DomainClassArtefactHandler.TYPE, cls)
        }
        def config = grailsApplication.config
        def dataSourceNames = AbstractMultipleDataSourceAggregatePersistenceContextInterceptor.calculateDataSourceNames(grailsApplication)
        def initializer = new HibernateDatastoreSpringInitializer((PropertyResolver)config, persistentClasses)
        if(dataSourceNames) {
            initializer.dataSources = dataSourceNames
        }

        def context = grailsApplication.getMainContext()
        def beansClosure = initializer.getBeanDefinitions((BeanDefinitionRegistry)context)
        runtime.putValue('initializedHibernatePersistentClasses', Collections.unmodifiableList(new ArrayList(persistentClasses)))
        defineBeans(runtime, immediateDelivery, beansClosure)
    }
    
    protected void initTransaction(TestRuntime runtime, Object target) {
        def transactionInterceptor = new GrailsTestTransactionInterceptor(getGrailsApplication(runtime).mainContext)
        if (runtime.containsValueFor("hibernateInterceptor") && transactionInterceptor.isTransactional(target)) {
            transactionInterceptor.init()
            runtime.putValue("hibernateTransactionInterceptor", transactionInterceptor)
        }
    }

    protected void destroyTransaction(TestRuntime runtime) {
        if (runtime.containsValueFor("hibernateTransactionInterceptor")) {
            def transactionInterceptor = runtime.removeValue("hibernateTransactionInterceptor", GrailsTestTransactionInterceptor)
            transactionInterceptor.destroy()
        }
    }

    void onTestEvent(TestEvent event) {
        switch(event.name) {
            case 'before':
                before(event.runtime, event.arguments.testInstance)
                break
            case 'after':
                after(event.runtime)
                break
            case 'registerBeans':
                registerBeans(event.runtime, (GrailsApplication)event.arguments.grailsApplication)
                break
            case 'afterClass':
                event.runtime.removeValue("hibernatePersistentClassesToRegister")
                break
            case 'beforeClass':
                Collection<Class<?>> persistentClasses = [] as Set
                persistentClasses.addAll(GormTestPluginUtil.collectDomainClassesFromAnnotations((Class<?>)event.arguments.testClass, event.runtime.getSharedRuntimeConfigurer()?.getClass()))
                event.runtime.putValue('hibernatePersistentClassesToRegister', persistentClasses)
                break
            case 'hibernateDomain':
                hibernateDomain(event.runtime, event.arguments)
                break
        }
    }
    
    void close(TestRuntime runtime) {
        runtime.removeValue('initializedHibernatePersistentClasses')
    }
}


