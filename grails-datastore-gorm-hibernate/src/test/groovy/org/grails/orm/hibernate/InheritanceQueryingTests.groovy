package org.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test
import static junit.framework.Assert.*

class InheritanceQueryingTests extends AbstractGrailsHibernateTests {

    @Test
    void testPolymorphicQuerying() {
        def city = InheritanceQueryingCity.newInstance(code: "LON", name: "London", longitude: 49.1, latitude: 53.1)
        def location = InheritanceQueryingLocation.newInstance(code: "XX", name: "The World")
        def country = InheritanceQueryingCountry.newInstance(code: "UK", name: "United Kingdom", population: 10000000)

        country.save()
        city.save()
        location.save()

        assertEquals 1, InheritanceQueryingCity.findAll().size()
        assertEquals 1, InheritanceQueryingCountry.findAll().size()
        assertEquals 3, InheritanceQueryingLocation.findAll().size()
    }

    @Override
    protected getDomainClasses() {
        [InheritanceQueryingVersioned, InheritanceQueryingLocation, InheritanceQueryingCity, InheritanceQueryingCountry]
    }
}


@Entity
class InheritanceQueryingCity extends InheritanceQueryingLocation {
    BigDecimal latitude
    BigDecimal longitude
}

@Entity
class InheritanceQueryingCountry extends InheritanceQueryingLocation {
    int population
}

@Entity
class InheritanceQueryingLocation extends InheritanceQueryingVersioned {
    String name
    String code
}

@Entity
abstract class InheritanceQueryingVersioned {
    Long id
    Long version
}

