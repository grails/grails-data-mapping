package org.grails.datastore.gorm

import grails.core.DefaultGrailsApplication
import groovy.sql.Sql
import grails.core.GrailsApplication
import grails.core.GrailsDomainClass
import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.orm.hibernate.GrailsHibernateTransactionManager
import org.codehaus.groovy.grails.orm.hibernate.GrailsSessionContext
import org.codehaus.groovy.grails.orm.hibernate.HibernateDatastore
import org.codehaus.groovy.grails.orm.hibernate.HibernateGormEnhancer
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsAnnotationConfiguration
import org.codehaus.groovy.grails.orm.hibernate.cfg.HibernateUtils
import org.codehaus.groovy.grails.orm.hibernate.events.PatchedDefaultFlushEventListener
import org.codehaus.groovy.grails.orm.hibernate.support.ClosureEventTriggeringInterceptor
import org.codehaus.groovy.grails.orm.hibernate.validation.HibernateConstraintsEvaluator
import org.codehaus.groovy.grails.orm.hibernate.validation.HibernateDomainClassValidator
import org.codehaus.groovy.grails.orm.hibernate.validation.PersistentConstraintFactory
import org.codehaus.groovy.grails.orm.hibernate.validation.UniqueConstraint
import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.grails.core.metaclass.MetaClassEnhancer

//import org.codehaus.groovy.grails.plugins.web.api.ControllersDomainBindingApi
import org.grails.datastore.gorm.config.GrailsDomainClassMappingContext
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.model.MappingContext
import org.h2.Driver
import org.hibernate.SessionFactory
import org.hibernate.cache.ehcache.EhCacheRegionFactory
import org.hibernate.cfg.AvailableSettings
import org.hibernate.dialect.H2Dialect
import org.springframework.beans.BeanUtils
import org.springframework.beans.BeanWrapper
import org.springframework.beans.BeanWrapperImpl
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.support.AbstractBeanDefinition
import org.springframework.beans.factory.support.GenericBeanDefinition
import org.springframework.context.ApplicationContext
import org.springframework.context.support.GenericApplicationContext
import org.springframework.orm.hibernate4.SessionFactoryUtils
import org.springframework.orm.hibernate4.SessionHolder
import org.springframework.orm.hibernate4.SpringSessionContext
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.DefaultTransactionDefinition
import org.springframework.transaction.support.TransactionSynchronizationManager

class Setup {
    static GrailsApplication grailsApplication
    static HibernateDatastore hibernateDatastore
    static hibernateSession
    static GrailsHibernateTransactionManager transactionManager
    static SessionFactory sessionFactory
    static TransactionStatus transactionStatus
    static GrailsAnnotationConfiguration hibernateConfig
    static ApplicationContext applicationContext

    @CompileStatic
    static destroy() {
        if (transactionStatus != null) {
            def tx = transactionStatus
            transactionStatus = null
            transactionManager.rollback(tx)
        }
        if (hibernateSession != null) {
            SessionFactoryUtils.closeSession( (org.hibernate.Session)hibernateSession )
        }

        if(hibernateConfig != null) {
            hibernateConfig = null
        }
        grailsApplication = null
        hibernateDatastore = null
        hibernateSession = null
        transactionManager = null
        sessionFactory = null
        if(applicationContext instanceof DisposableBean) {
            applicationContext.destroy()
        }
        applicationContext = null
        shutdownInMemDb()
    }
    
    static shutdownInMemDb() {
        Sql sql = null
        try {
            sql = Sql.newInstance('jdbc:h2:mem:devDB', 'sa', '', Driver.name)
            sql.executeUpdate('SHUTDOWN')
        } catch (e) {
            // already closed, ignore
        } finally {
            try { sql?.close() } catch (ignored) {}
        }
    }

    static Session setup(List<Class> classes, ConfigObject grailsConfig = null, boolean isTransactional = true) {
        ExpandoMetaClass.enableGlobally()
//        Log4jConfigurer.initLogging("classpath:log4j.properties")

        grailsApplication = new DefaultGrailsApplication(classes as Class[], new GroovyClassLoader(Setup.getClassLoader()))
        if(grailsConfig) {
            grailsApplication.config.putAll(grailsConfig)
        }

        applicationContext = new GenericApplicationContext()
        def ctx = applicationContext

        grailsApplication.applicationContext = ctx
        grailsApplication.mainContext = ctx
        grailsApplication.initialise()
        ctx.beanFactory.registerSingleton 'grailsApplication', grailsApplication

        for (GrailsDomainClass dc in grailsApplication.domainClasses) {
            if (!dc.abstract) {
                ctx.registerBeanDefinition dc.clazz.name, new GenericBeanDefinition(
                        autowireMode: AbstractBeanDefinition.AUTOWIRE_BY_NAME,
                        beanClass: dc.clazz,
                        scope: BeanDefinition.SCOPE_PROTOTYPE)
            }
        }
        ctx.refresh()

        def config = new Properties()
        config.setProperty AvailableSettings.DIALECT, H2Dialect.name
        config.setProperty AvailableSettings.DRIVER, Driver.name
        config.setProperty AvailableSettings.URL, "jdbc:h2:mem:devDB;MVCC=true;INIT=CREATE SCHEMA IF NOT EXISTS WWW;"
        config.setProperty AvailableSettings.USER, "sa"
        config.setProperty AvailableSettings.PASS, ""
        config.setProperty AvailableSettings.HBM2DDL_AUTO, "create-drop"
        config.setProperty AvailableSettings.SHOW_SQL, "true"
        config.setProperty AvailableSettings.FORMAT_SQL, "true"
        config.setProperty AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, SpringSessionContext.name
        config.setProperty AvailableSettings.USE_SECOND_LEVEL_CACHE, "true"
        config.setProperty AvailableSettings.USE_QUERY_CACHE, "true"
        config.setProperty AvailableSettings.CACHE_REGION_FACTORY, EhCacheRegionFactory.name
        config.setProperty AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, GrailsSessionContext.name

        hibernateConfig = new GrailsAnnotationConfiguration()
        hibernateConfig.setProperties config

        def eventTriggeringInterceptor = new ClosureEventTriggeringInterceptor(applicationContext: ctx)
        hibernateConfig.setEventListeners(
            ['pre-load': eventTriggeringInterceptor,
            'flush': new PatchedDefaultFlushEventListener(),
             'save': eventTriggeringInterceptor,
             'post-load': eventTriggeringInterceptor,
             'save-update': eventTriggeringInterceptor,
             'pre-insert': eventTriggeringInterceptor,
             'post-insert': eventTriggeringInterceptor,
             'pre-update': eventTriggeringInterceptor,
             'pre-delete': eventTriggeringInterceptor,
             'post-update': eventTriggeringInterceptor,
             'post-delete': eventTriggeringInterceptor ] )



        hibernateConfig.grailsApplication = grailsApplication

        def context = new GrailsDomainClassMappingContext(grailsApplication)
        ctx.beanFactory.registerSingleton 'grailsDomainClassMappingContext', context

        sessionFactory = hibernateConfig.buildSessionFactory()
        ctx.beanFactory.registerSingleton 'sessionFactory', sessionFactory

        transactionManager = new GrailsHibernateTransactionManager(sessionFactory: sessionFactory)
        ctx.beanFactory.registerSingleton 'transactionManager', transactionManager

        hibernateDatastore = new HibernateDatastore(context, sessionFactory, grailsApplication.config, ctx)
        ctx.beanFactory.registerSingleton 'hibernateDatastore', hibernateDatastore

        eventTriggeringInterceptor.datastores = [(sessionFactory): hibernateDatastore]
        ctx.beanFactory.registerSingleton 'eventTriggeringInterceptor', eventTriggeringInterceptor

        def metaClassEnhancer = new MetaClassEnhancer()
//        metaClassEnhancer.addApi new ControllersDomainBindingApi()

        HibernateConstraintsEvaluator evaluator = new HibernateConstraintsEvaluator()
        grailsApplication.domainClasses.each { GrailsDomainClass dc ->
            if (dc.abstract) {
                return
            }

            metaClassEnhancer.enhance dc.metaClass


            def validator = new HibernateDomainClassValidator()
            validator.sessionFactory = sessionFactory
            validator.grailsApplication = grailsApplication
            validator.domainClass = dc
            validator.messageSource = ctx
            dc.validator = validator

            dc.metaClass.constructor = { ->
                def obj
                if (ctx.containsBean(dc.fullName)) {
                    obj = ctx.getBean(dc.fullName)
                }
                else {
                    obj = BeanUtils.instantiateClass(dc.clazz)
                }
                obj
            }
        }

        def enhancer = new HibernateGormEnhancer(hibernateDatastore, transactionManager, grailsApplication)
        enhancer.enhance()

        hibernateDatastore.mappingContext.addMappingContextListener({ e ->
            enhancer.enhance e
        } as MappingContext.Listener)

        transactionManager = new GrailsHibernateTransactionManager(sessionFactory: sessionFactory)
        if (transactionStatus == null && isTransactional) {
            transactionStatus = transactionManager.getTransaction(new DefaultTransactionDefinition())
        }
        else if(isTransactional){
            throw new RuntimeException("new transaction started during active transaction")
        }

        if(!isTransactional) {
            hibernateSession = sessionFactory.openSession()
            TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(hibernateSession))
        }
        else {
            hibernateSession = sessionFactory.currentSession
        }

        ApplicationContext.metaClass.getProperty = { String name ->
            if (delegate.containsBean(name)) {
                return delegate.getBean(name)
            }
            BeanWrapper bw = new BeanWrapperImpl(delegate)
            if (bw.isReadableProperty(name)) {
                return bw.getPropertyValue(name)
            }
        }

        HibernateUtils.enhanceSessionFactories(ctx, grailsApplication)

        ConstrainedProperty.registerNewConstraint(UniqueConstraint.UNIQUE_CONSTRAINT,
                new PersistentConstraintFactory(ctx, UniqueConstraint))

        return hibernateDatastore.connect()
    }
}
