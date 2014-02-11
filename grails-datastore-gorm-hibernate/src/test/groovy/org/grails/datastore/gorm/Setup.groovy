package org.grails.datastore.gorm

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.metaclass.MetaClassEnhancer
import org.codehaus.groovy.grails.orm.hibernate.GrailsHibernateTransactionManager
import org.codehaus.groovy.grails.orm.hibernate.HibernateDatastore
import org.codehaus.groovy.grails.orm.hibernate.HibernateGormEnhancer
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsAnnotationConfiguration
import org.codehaus.groovy.grails.orm.hibernate.cfg.HibernateUtils
import org.codehaus.groovy.grails.orm.hibernate.support.ClosureEventTriggeringInterceptor
import org.codehaus.groovy.grails.orm.hibernate.validation.HibernateConstraintsEvaluator
import org.codehaus.groovy.grails.orm.hibernate.validation.PersistentConstraintFactory
import org.codehaus.groovy.grails.orm.hibernate.validation.UniqueConstraint
//import org.codehaus.groovy.grails.plugins.web.api.ControllersDomainBindingApi
import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.grails.datastore.gorm.config.GrailsDomainClassMappingContext
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.model.MappingContext
import org.h2.Driver
import org.hibernate.SessionFactory
import org.hibernate.cfg.Environment
import org.hibernate.dialect.H2Dialect
import org.springframework.beans.BeanUtils
import org.springframework.beans.BeanWrapper
import org.springframework.beans.BeanWrapperImpl
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.support.AbstractBeanDefinition
import org.springframework.beans.factory.support.GenericBeanDefinition
import org.springframework.context.ApplicationContext
import org.springframework.context.support.GenericApplicationContext
import org.springframework.orm.hibernate3.SessionFactoryUtils
import org.springframework.orm.hibernate3.SpringSessionContext
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.DefaultTransactionDefinition
import org.springframework.validation.Errors
import org.springframework.validation.Validator
import org.springframework.util.Log4jConfigurer

class Setup {
    static HibernateDatastore hibernateDatastore
    static hibernateSession
    static GrailsHibernateTransactionManager transactionManager
    static TransactionStatus transactionStatus
    static GrailsAnnotationConfiguration hibernateConfig

    static destroy() {
        if (hibernateSession != null) {
            SessionFactoryUtils.releaseSession hibernateSession, hibernateDatastore.sessionFactory
        }
        if (transactionStatus) {
            transactionManager.rollback(transactionStatus)
            transactionStatus = null
        }
        if(hibernateConfig != null) {
            hibernateConfig = null
        }
    }

    static Session setup(List<Class> classes) {
//        Log4jConfigurer.initLogging("classpath:log4j.properties")

        def grailsApplication = new DefaultGrailsApplication(classes as Class[], Setup.getClassLoader())
        def ctx = new GenericApplicationContext()

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

        hibernateConfig = new GrailsAnnotationConfiguration()
        hibernateConfig.setProperties config

        def eventTriggeringInterceptor = new ClosureEventTriggeringInterceptor(applicationContext: ctx)
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

        hibernateConfig.grailsApplication = grailsApplication

        def context = new GrailsDomainClassMappingContext(grailsApplication)
        ctx.beanFactory.registerSingleton 'grailsDomainClassMappingContext', context

        SessionFactory sessionFactory = hibernateConfig.buildSessionFactory()
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

            def validator = [supports: { Class c -> true}, validate: { target, Errors errors ->
                for (ConstrainedProperty cp in evaluator.evaluate(dc.clazz).values()) {
                    cp.validate(target, target[cp.propertyName], errors)
                }
            }] as Validator

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
        if (transactionStatus == null) {
            transactionStatus = transactionManager.getTransaction(new DefaultTransactionDefinition())
        }
        else {
            throw new RuntimeException("new transaction started during active transaction")
        }

        hibernateSession = SessionFactoryUtils.doGetSession(sessionFactory, true)

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
