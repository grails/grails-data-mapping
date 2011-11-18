package grails.gorm.tests

import grails.persistence.Entity

/**
 * @author graemerocher
 */
class InheritanceSpec extends GormDatastoreSpec {

    void "Test inheritance with dynamic finder"() {

        given:
            def city = new City([code: "UK", name: "London", longitude: 49.1, latitude: 53.1])
            def country = new Country([code: "UK", name: "United Kingdom", population: 10000000])

            city.save()
            country.save(flush:true)
            session.clear()

        when:
            def locations = Location.findAllByCode("UK")
            def cities = City.findAllByCode("UK")
            def countries = Country.findAllByCode("UK")

        then:
            2 == locations.size()
            1 == cities.size()
            1 == countries.size()
            "London" == cities[0].name
            "United Kingdom" == countries[0].name
    }

    void "Test querying with inheritance"() {

        given:
            def city = new City([code: "LON", name: "London", longitude: 49.1, latitude: 53.1])
            def location = new Location([code: "XX", name: "The World"])
            def country = new Country([code: "UK", name: "United Kingdom", population: 10000000])

            country.save()
            city.save()
            location.save()

            session.flush()

        when:
            city = City.get(city.id)
            def london = Location.get(city.id)
            country = Location.findByName("United Kingdom")
            def london2 = Location.findByName("London")

        then:
            1 == City.count()
            1 == Country.count()
            3 == Location.count()

            city != null
            city instanceof City
            london instanceof City
            london2 instanceof City
            "London" == london2.name
            49.1 == london2.longitude
            "LON" == london2.code

            country instanceof Country
            "UK" == country.code
            10000000 == country.population
    }

    def clearSession() {
        City.withSession { session -> session.flush(); }
    }
}

@Entity
class Location implements Serializable {
    Long id
    Long version
    String name
    String code = "DEFAULT"

    def namedAndCode() {
        "$name - $code"
    }

    static mapping = {
        name index:true
        code index:true
    }
}

@Entity
class City extends Location {
    BigDecimal latitude
    BigDecimal longitude
}

@Entity
class Country extends Location {
    Integer population

    static hasMany = [residents:Person]
    Set residents
}
