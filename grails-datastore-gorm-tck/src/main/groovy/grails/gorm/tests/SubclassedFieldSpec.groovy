package grails.gorm.tests

import grails.gorm.tests.GormDatastoreSpec
import grails.gorm.tests.City
import grails.gorm.tests.Location
import grails.persistence.Entity

/**
 * Abstract base test for order by queries. Subclasses should do the necessary setup to configure GORM
 */
class SubclassedFieldSpec extends GormDatastoreSpec {

    void "sub-classes in fields not casted correctly"() {
        given:"Some test data"
	    City city = new City(name:'MyCity', latitude: 54.323, longitude: 10.139).save(flush: true, failOnError: true)
	    PersonWithLocation person = new PersonWithLocation(name:'Timmi Tester', location: city).save(flush: true, failOnError: true)

            session.flush()
	    session.clear()
	    
	when:
	    def p  = PersonWithLocation.findAll()
	    def location = p[0].location

        then:
            location instanceof City
    }

    @Override
    List getDomainClasses() {
        [City, PersonWithLocation, Location]
    }
}

@Entity
class PersonWithLocation {
    String id
    String name
    Location location
}
