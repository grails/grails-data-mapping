package org.codehaus.groovy.grails.orm.hibernate

import org.junit.Test
import static junit.framework.Assert.*

/**
 * @author Burt Beckwith
 */
class IndexedPropertyTests extends AbstractGrailsHibernateTests {

    // test for GRAILS-5999
    @Test
    void testIndexedProperty() {
        assertNotNull Eyjafjallajokul.newInstance(name: 'volcano').save(flush: true)
        session.clear()

        assertEquals 1, Eyjafjallajokul.count()
    }

    @Override
    protected getDomainClasses() {
        [Eyjafjallajokul]
    }
}


class Eyjafjallajokul {
    Long id
    Long version
    String name
    int getFoo(int bar) { 0 }
}
