package org.codehaus.groovy.grails.orm.hibernate

import static junit.framework.Assert.*
import org.junit.Test

class MappedByColumn2Tests extends AbstractGrailsHibernateTests {

    @Override
    protected getDomainClasses() {
        [Airport2, Route2]
    }

    @Test
    void testWithConfig() {
        def airportClass = ga.getDomainClass(Airport2.name)
        def routeClass = ga.getDomainClass(Route2.name)

        def a = airportClass.newInstance()

        a.save()

        def r = routeClass.newInstance()
        a.addToRoutes(r)

        a.save()

        assertEquals 1, a.routes.size()
        assertEquals a, r.destination

        assertNull r.airport
    }

}

class Airport2 {
    Long id
    Long version
    Set routes

    static mappedBy = [routes:'destination']
    static hasMany = [routes:Route2]
}

class Route2 {
    Long id
    Long version

    Airport2 airport
    Airport2 destination

    static constraints = {
        airport nullable:true
    }
}

