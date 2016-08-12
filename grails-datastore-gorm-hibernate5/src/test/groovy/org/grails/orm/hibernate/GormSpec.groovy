package org.grails.orm.hibernate

import grails.core.GrailsApplication
import grails.gorm.tests.GormDatastoreSpec
import org.grails.datastore.mapping.core.Session
import org.hibernate.SessionFactory
import org.springframework.context.ApplicationContext
import spock.lang.Shared

abstract class GormSpec extends GormDatastoreSpec {

    GrailsApplication grailsApplication
    ApplicationContext applicationContext
    SessionFactory sessionFactory

    @Shared List savedTestClasses
    def setupSpec() {
        savedTestClasses = new ArrayList(TEST_CLASSES)
        TEST_CLASSES.clear()
    }

    def cleanupSpec() {
        TEST_CLASSES.addAll(savedTestClasses)
    }

    def setup() {
        grailsApplication = setupClass.grailsApplication
        applicationContext = setupClass.applicationContext
        sessionFactory = setupClass.sessionFactory
    }

    @Override
    Session createSession() {
        setupClass.setup(((TEST_CLASSES + getDomainClasses()) as Set) as List, new ConfigObject(['hibernate': ['hbm2ddl.auto': 'update']]))
    }
}
