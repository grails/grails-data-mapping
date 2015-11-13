package org.grails.datastore.gorm

import grails.core.DefaultGrailsApplication
import grails.validation.ConstraintsEvaluator
import groovy.sql.Sql
import grails.core.GrailsApplication
import grails.core.GrailsDomainClass
import grails.validation.ConstrainedProperty
import groovy.transform.CompileStatic
import org.grails.orm.hibernate.GrailsHibernateTransactionManager
import org.grails.orm.hibernate.HibernateDatastore
import org.grails.orm.hibernate.HibernateGormEnhancer
import org.grails.orm.hibernate.cfg.HibernateMappingContext
import org.grails.orm.hibernate.cfg.HibernateMappingContextConfiguration
import org.grails.orm.hibernate.cfg.HibernateUtils
import org.grails.orm.hibernate.events.PatchedDefaultFlushEventListener
import org.grails.orm.hibernate.proxy.HibernateProxyHandler
import org.grails.orm.hibernate.support.ClosureEventTriggeringInterceptor
import org.grails.orm.hibernate.validation.HibernateConstraintsEvaluator
import org.grails.orm.hibernate.validation.HibernateDomainClassValidator
import org.grails.orm.hibernate.validation.PersistentConstraintFactory
import org.grails.orm.hibernate.validation.UniqueConstraint
import org.grails.core.metaclass.MetaClassEnhancer

//import org.codehaus.groovy.grails.plugins.web.api.ControllersDomainBindingApi
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.model.MappingContext
import org.h2.Driver
import org.hibernate.SessionFactory
import org.hibernate.cache.EhCacheRegionFactory
import org.hibernate.cfg.Environment
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
import org.springframework.orm.hibernate3.SessionFactoryUtils
import org.springframework.orm.hibernate3.SessionHolder
import org.springframework.orm.hibernate3.SpringSessionContext
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
    static HibernateMappingContextConfiguration hibernateConfig
    static ApplicationContext applicationContext

    @CompileStatic
    static destroy() {
        if (hibernateSession != null) {
            SessionFactoryUtils.releaseSession( (org.hibernate.Session)hibernateSession, hibernateDatastore.sessionFactory )
        }
        if (transactionStatus) {
            transactionManager.rollback(transactionStatus)
            transactionStatus = null
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
            grailsApplication.config.merge(grailsConfig)
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
        config.setProperty Environment.DIALECT, H2Dialect.name
        config.setProperty Environment.DRIVER, Driver.name
        config.setProperty Environment.URL, "jdbc:h2:mem:devDB;MVCC=true;INIT=CREATE SCHEMA IF NOT EXISTS WWW;"
        config.setProperty Environment.USER, "sa"
        config.setProperty Environment.PASS, ""
        config.setProperty Environment.HBM2DDL_AUTO, "create-drop"
        config.setProperty Environment.SHOW_SQL, "true"
        config.setProperty Environment.FORMAT_SQL, "true"
        config.setProperty Environment.CURRENT_SESSION_CONTEXT_CLASS, SpringSessionContext.name
        config.setProperty Environment.USE_SECOND_LEVEL_CACHE, "true"
        config.setProperty Environment.USE_QUERY_CACHE, "true"
        config.setProperty Environment.CACHE_REGION_FACTORY, EhCacheRegionFactory.name

        Closure defaultMapping = grailsConfig?.grails?.gorm?.default?.mapping instanceof Closure ? grailsConfig.grails.gorm.default.mapping : null
        def context = new HibernateMappingContext((Closure)defaultMapping, ctx)
        context.addPersistentEntities(*grailsApplication.domainClasses*.clazz)
        context.setProxyFactory(new HibernateProxyHandler())
        ctx.beanFactory.registerSingleton 'grailsDomainClassMappingContext', context

        hibernateConfig = new HibernateMappingContextConfiguration()
        hibernateConfig.setHibernateMappingContext(context)
        hibernateConfig.setProperties config

        def eventTriggeringInterceptor = new ClosureEventTriggeringInterceptor(applicationContext: ctx)
        hibernateConfig.setListener('flush', new PatchedDefaultFlushEventListener())
        hibernateConfig.setListener 'pre-load', eventTriggeringInterceptor
        hibernateConfig.setListener 'post-load', eventTriggeringInterceptor
        hibernateConfig.setListener 'save', eventTriggeringInterceptor
        hibernateConfig.setListener 'save-update', eventTriggeringInterceptor
        hibernateConfig.setListener 'pre-insert', eventTriggeringInterceptor
        hibernateConfig.setListener 'post-insert', eventTriggeringInterceptor
        hibernateConfig.setListener 'pre-update', eventTriggeringInterceptor
        hibernateConfig.setListener 'pre-delete', eventTriggeringInterceptor
        hibernateConfig.setListener 'post-update', eventTriggeringInterceptor
        hibernateConfig.setListener 'post-delete', eventTriggeringInterceptor


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
        evaluator.setMappingContext(hibernateDatastore.mappingContext)
        ctx.beanFactory.registerSingleton(ConstraintsEvaluator.BEAN_NAME, evaluator)
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
            def entity = context.getPersistentEntity(dc.fullName)
            if(entity != null) {
                context.addEntityValidator(entity, validator)
            }

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

        def enhancer = new HibernateGormEnhancer(hibernateDatastore, transactionManager)
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

        hibernateSession = SessionFactoryUtils.doGetSession(sessionFactory, true)
        if(!isTransactional) {
            TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(hibernateSession))
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
