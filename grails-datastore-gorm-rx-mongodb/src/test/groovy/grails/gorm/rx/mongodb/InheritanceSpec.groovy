package grails.gorm.rx.mongodb

import grails.gorm.rx.mongodb.domains.City
import grails.gorm.rx.mongodb.domains.Country
import grails.gorm.rx.mongodb.domains.Location

class InheritanceSpec extends RxGormSpec {

    @Override
    List<Class> getDomainClasses() {
        [City, Country, Location]
    }

    void "Test inheritance with dynamic finder"() {

        given:
        def city = new City([code: "UK", name: "London", longitude: 49.1, latitude: 53.1])
        def country = new Country([code: "UK", name: "United Kingdom", population: 10000000])

        city.save().toBlocking().first()
        country.save(flush:true).toBlocking().first()

        when:
        def locations = Location.findAllByCode("UK").toList().toBlocking().first()
        def cities = City.findAllByCode("UK").toList().toBlocking().first()
        def countries = Country.findAllByCode("UK").toList().toBlocking().first()

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

        country.save().toBlocking().first()
        city.save().toBlocking().first()
        location.save().toBlocking().first()


        when:
        city = City.get(city.id).toBlocking().first()
        def london = Location.get(city.id).toBlocking().first()
        country = Location.findByName("United Kingdom").toBlocking().first()
        def london2 = Location.findByName("London").toBlocking().first()

        then:
        1 == City.count().toBlocking().first()
        1 == Country.count().toBlocking().first()
        3 == Location.count().toBlocking().first()

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
}
