package org.grails.orm.hibernate

import grails.persistence.Entity

import org.codehaus.groovy.grails.commons.test.*
import org.junit.Test

import static junit.framework.Assert.*

class DeepHierarchyTests extends AbstractGrailsHibernateTests {

    @Test
    void testCountInDeepHierarchy() {
        def p1 = Personnel.newInstance()
        p1.name = "Joe Bloggs"
        p1.save()
        session.flush()

        def a1 = Approver.newInstance()
        a1.name = "Fred Flinstone"
        a1.status = "active"
        a1.save()
        session.flush()

        def h1 = Handler.newInstance()
        h1.name = "Barney Rubble"
        h1.status = "dormant"
        h1.strength = 10
        h1.save()
        session.flush()

        session.clear()

        assertEquals 3, Personnel.count()
        assertEquals 2, Approver.count()
        assertEquals 1, Handler.count()
    }

    @Test
    void testPersistentValuesInDeepHierarchy() {
        def p1 = Personnel.newInstance()
        p1.name = "Joe Bloggs"
        p1.save()
        session.flush()

        def a1 = Approver.newInstance()
        a1.name = "Fred Flinstone"
        a1.status = "active"
        a1.save()
        session.flush()

        def h1 = Handler.newInstance()
        h1.name = "Barney Rubble"
        h1.status = "dormant"
        h1.strength = 10
        h1.save()
        session.flush()

        def aId = a1.id
        def pId = p1.id
        def hId = h1.id

        session.clear()

        def p2 = Personnel.get(pId)
        assertEquals "Joe Bloggs", p2.name

        def a2 = Approver.get(aId)
        assertEquals "Fred Flinstone", a2.name
        assertEquals "active", a2.status

        def h2 = Handler.get(hId)
        assertEquals "Barney Rubble", h2.name
        assertEquals "dormant", h2.status
        assertEquals 10, h2.strength
    }

    @Override
    protected getDomainClasses() {
        [Personnel, Approver, Handler]
    }
}
@Entity
class Personnel {
    Long id
    Long version
    String name
}

@Entity
class Approver extends Personnel {
    Long id
    Long version
    String status
}

@Entity
class Handler extends Approver {
    Long id
    Long version
    int strength
}
