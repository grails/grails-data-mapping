package org.grails.orm.hibernate

import static junit.framework.Assert.*
import grails.persistence.Entity;

import org.junit.Test


class MappedByColumnTests extends AbstractGrailsHibernateTests {

    @Test
    void testByConvention() {
        def a = Airport.newInstance()

        a.save()

        def r = Route.newInstance()
        a.addToRoutes(r)

        a.save()

        assertEquals 1, a.routes.size()
        assertEquals a, r.airport

        assertNull r.destination
    }

    @Test
    void testOtherPropertyWithConvention() {
        def a = Airport.newInstance()

        a.save()

        def r = Route.newInstance()
        r.destination = a

        r.save()

        assertNotNull r.destination.id
    }

    @Override
    protected getDomainClasses() {
        [Airport, Route]
    }
}

@Entity
class Airport {
    Long id
    Long version
    Set routes

    static hasMany = [routes:Route]
}

@Entity
class Route {
    Long id
    Long version

    Airport airport
    Airport destination

    static constraints = {
        airport(nullable:true)
        destination(nullable:true)
    }
}

