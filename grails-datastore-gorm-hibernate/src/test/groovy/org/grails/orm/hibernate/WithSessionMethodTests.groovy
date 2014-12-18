package org.codehaus.groovy.grails.orm.hibernate

import grails.gorm.tests.Plant
import org.hibernate.Session

import static junit.framework.Assert.*
import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Sep 4, 2008
 */
class WithSessionMethodTests extends AbstractGrailsHibernateTests {

    @Override
    protected getDomainClasses() {
        [Plant]
    }

    @Test
    void testWithSessionMethod() {

        Session testSession
        Plant.withSession { Session session ->
            testSession = session
        }

        assertNotNull testSession
    }

    @Test
    void testWithNewSessionMethod() {


        Session testSession
        def returnValue = Plant.withNewSession { Session session ->
            testSession = session
            5
        }

        assertNotNull testSession
        assertEquals 5, returnValue
    }

}
