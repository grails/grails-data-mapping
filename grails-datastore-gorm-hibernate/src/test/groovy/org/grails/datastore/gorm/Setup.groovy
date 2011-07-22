package org.grails.datastore.gorm

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsAnnotationConfiguration
import org.codehaus.groovy.grails.orm.hibernate.support.ClosureEventTriggeringInterceptor
import org.codehaus.groovy.grails.validation.GrailsDomainClassValidator
import org.grails.datastore.gorm.config.GrailsDomainClassMappingContext
import org.grails.datastore.gorm.hibernate.HibernateDatastore
import org.grails.datastore.gorm.hibernate.HibernateGormEnhancer
import org.hibernate.dialect.HSQLDialect
import org.hsqldb.jdbcDriver
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.springframework.orm.hibernate3.HibernateTransactionManager
import org.springframework.orm.hibernate3.SessionFactoryUtils
import org.springframework.orm.hibernate3.SpringSessionContext
import org.springframework.transaction.support.TransactionSynchronizationManager

class Setup {
    static hibernateDatastore
    static hibernateSession

    static destroy() {
        TransactionSynchronizationManager.clear()
        if (hibernateSession != null) {
            SessionFactoryUtils.releaseSession hibernateSession, hibernateDatastore.sessionFactory
        }
        HibernateDatastore.retrieveSession().disconnect()
    }

    static Session setup(classes) {

        def grailsApplication = new DefaultGrailsApplication(classes as Class[], Setup.getClassLoader())
        grailsApplication.initialise()

        def config = new Properties()
        config.setProperty "hibernate.dialect", HSQLDialect.name
        config.setProperty "hibernate.connection.driver_class", jdbcDriver.name
        config.setProperty "hibernate.connection.url", "jdbc:hsqldb:mem:devDB"
        config.setProperty "hibernate.connection.username", "sa"
        config.setProperty "hibernate.connection.password", ""
        config.setProperty "hibernate.hbm2ddl.auto", "create-drop"
        config.setProperty "hibernate.show_sql", "true"
        config.setProperty "hibernate.format_sql", "true"
        config.setProperty "hibernate.current_session_context_class", SpringSessionContext.name

        def hibernateConfig = new GrailsAnnotationConfiguration()
        hibernateConfig.setProperties config
        def listener = new ClosureEventTriggeringInterceptor()
        hibernateConfig.setListener 'pre-load', listener
        hibernateConfig.setListener 'post-load', listener
        hibernateConfig.setListener 'save', listener
        hibernateConfig.setListener 'save-update', listener
        hibernateConfig.setListener 'post-insert', listener
        hibernateConfig.setListener 'pre-update', listener
        hibernateConfig.setListener 'pre-delete', listener
        hibernateConfig.setListener 'post-update', listener
        hibernateConfig.setListener 'post-delete', listener

        hibernateConfig.setGrailsApplication grailsApplication

        def context = new GrailsDomainClassMappingContext(grailsApplication)

        def sessionFactory = hibernateConfig.buildSessionFactory()

        def txMgr = new HibernateTransactionManager(sessionFactory)
        hibernateDatastore = new HibernateDatastore(context, sessionFactory)

        PersistentEntity entity = hibernateDatastore.mappingContext.persistentEntities.find { PersistentEntity e -> e.name.contains("TestEntity")}

        def validator = new GrailsDomainClassValidator()

        validator.grailsApplication = grailsApplication
        def domainClass = grailsApplication.getDomainClass(entity.name)

        validator.domainClass = domainClass

        hibernateDatastore.mappingContext.addEntityValidator(entity, validator)

        def enhancer = new HibernateGormEnhancer(hibernateDatastore, txMgr)
        enhancer.enhance()

        hibernateDatastore.mappingContext.addMappingContextListener({ e ->
            enhancer.enhance e
        } as MappingContext.Listener)

        TransactionSynchronizationManager.initSynchronization()
        this.hibernateSession = SessionFactoryUtils.doGetSession(sessionFactory, true)
        def session = hibernateDatastore.connect()
        return session
    }
}
