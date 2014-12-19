package org.grails.orm.hibernate

import grails.core.GrailsApplication
import grails.gorm.tests.GormDatastoreSpec
import junit.framework.Assert
import org.grails.datastore.mapping.core.DatastoreUtils
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.junit.After
import org.junit.Before
import org.springframework.context.ApplicationContext
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus

/**
 * @author Graeme Rocher
 * @since 1.0
 */
abstract class AbstractGrailsHibernateTests {

    static final SETUP_CLASS_NAME = 'org.grails.datastore.gorm.Setup'


    static Class setupClass = GormDatastoreSpec.loadSetupClass()
    HibernateDatastore hibernateDatastore
    PlatformTransactionManager transactionManager
    GrailsApplication ga
    Session session
    HibernateSession hibernateSession
    TransactionStatus transactionStatus
    SessionFactory sessionFactory
    ApplicationContext applicationContext

    @Before
    void setUp() {
        hibernateSession = setupClass.setup(getDomainClasses(), config)
        transactionStatus = setupClass.transactionStatus
        session = setupClass.hibernateSession
        ga = setupClass.grailsApplication
        DatastoreUtils.bindSession hibernateSession
        hibernateDatastore = setupClass.hibernateDatastore
        transactionManager = (PlatformTransactionManager)setupClass.transactionManager
        sessionFactory = setupClass.sessionFactory
        applicationContext = setupClass.applicationContext

        onSetUp()
    }

	protected void onSetUp() {}



    @After
	void tearDown() {
        if (hibernateSession) {
            hibernateSession.disconnect()
            DatastoreUtils.unbindSession hibernateSession
        }
        try {
            setupClass.destroy()
        } catch(e) {
            println "ERROR: Exception during test cleanup: ${e.message}"
        }

	    onTearDown()
	}

	protected void onTearDown() {
	}


    /**
     * Subclasses may override this method to return a list of classes which should
     * be added to the GrailsApplication as domain classes
     *
     * @return a list of classes
     */
    protected getDomainClasses() {
        Collections.EMPTY_LIST
    }

    protected GrailsApplication getGrailsApplication() { ga }

    protected ConfigObject getConfig() { null }


    String shouldFail(Closure callable) {
        try {
            callable.call()
            Assert.fail("Should have failed, but didn't")
        } catch (Throwable e) {
            // ok
            return e.message
        }
    }
    String shouldFail(Class exceptionClass, Closure callable) {
        try {
            callable.call()
            Assert.fail "Should have thrown $exceptionClass, but didn't throw anything."
        } catch (e) {
            if(!exceptionClass.isInstance(e)) {
                Assert.fail "Should have thrown $exceptionClass instead threw ${e.getClass()}"
            }
            else {
                return e.message
            }
        }
    }
}
