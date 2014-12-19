package org.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test

import static junit.framework.Assert.*

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class DoNotPersistInvalidObjectTests extends AbstractGrailsHibernateTests {

    @Test
    void testDoNoPersistInvalidInstanceUsingDirtyChecking() {
        def t = DoNotPersist.newInstance(name:"bob")

        assertNotNull "should have saved test instance",t.save(flush:true)

        session.clear()

        t = DoNotPersist.get(1)
        t.name = "fartooolong"

        session.flush()
        session.clear()

        t = DoNotPersist.get(1)
        assertEquals "bob", t.name
    }

    @Test
    void testPersistValidInstanceUsingDirtyChecking() {
        def t = DoNotPersist.newInstance(name:"bob")

        assertNotNull "should have saved test instance",t.save(flush:true)

        session.clear()

        t = DoNotPersist.get(1)
        t.name = "fred"

        session.flush()
        session.clear()

        t = DoNotPersist.get(1)
        assertEquals "fred", t.name
    }

    @Override
    protected getDomainClasses() {
        [DoNotPersist]
    }
}


@Entity
class DoNotPersist {
    Long id
    Long version

    String name

    static constraints = {
        name size:1..5
    }
}

