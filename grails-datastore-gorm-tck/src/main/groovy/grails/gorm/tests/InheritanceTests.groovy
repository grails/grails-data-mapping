package grails.gorm.tests

import org.junit.Test

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Aug 30, 2010
 * Time: 12:53:23 PM
 * To change this template use File | Settings | File Templates.
 */
class InheritanceTests {

    @Test
    void testPolymorphicQuerying() {

        def city = new City([code: "LON", name: "London", longitude: 49.1, latitude: 53.1])
        def location = new Location([code: "XX", name: "The World"])
        def country = new Country([code: "UK", name: "United Kingdom", population: 10000000])

        country.save()
        city.save()
        location.save()

        City.withSession { session -> session.flush();session.clear() }
        

        assert 1 == City.count()
        assert 1 == Country.count()
        assert 3 == Location.count()

        city = City.get(city.id)

        assert city
        assert city instanceof City

        clearSession()

        city = Location.get(city.id)

        assert city
        assert city instanceof City

        clearSession()


        city = Location.findByName("London")
        assert city
        assert city instanceof City
        assert "London" == city.name
        assert 49.1 == city.longitude
        assert "LON" == city.code

        country = Location.findByName("United Kingdom")
        assert country
        assert country instanceof Country
        assert "UK" == country.code
        assert 10000000 == country.population

    }

  def clearSession() {
    City.withSession { session -> session.flush(); }
  }
}

class Location {
  Long id
  String name
  String code

  static mapping = {
    name index:true
  }
}
class City extends Location {
    BigDecimal latitude
    BigDecimal longitude
}

class Country extends Location {
  Integer population
}
