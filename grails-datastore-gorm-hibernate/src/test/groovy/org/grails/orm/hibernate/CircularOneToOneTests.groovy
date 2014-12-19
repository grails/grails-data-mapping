package org.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test

import static junit.framework.Assert.*

/**
 * @author Graeme Rocher
 */
class CircularOneToOneTests extends AbstractGrailsHibernateTests {

    @Test
    void testCircularOneToOne() {
        def test1 = CircularOneToOnePerson.newInstance()
        def test2 = CircularOneToOnePerson.newInstance()
        assertNotNull test1.save(flush:true)

        test2.creator = test1
        assertNotNull test2.save(flush:true)

        session.clear()

        test2 = CircularOneToOnePerson.get(2)
        assertNotNull test2
        assertNotNull test2.creator
    }

    @Override
    protected getDomainClasses() {
        [CircularOneToOnePerson]
    }
}

@Entity
class CircularOneToOnePerson {
    Long id
    Long version
    CircularOneToOnePerson creator

    static constraints = {
        creator nullable:true
    }
}

