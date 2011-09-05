package grails.gorm.tests

import spock.lang.Specification

/**
 * Tests for querying the size of collections etc.
 */
class SizeQuerySpec extends GormDatastoreSpec{


    void "Test sizeEq criterion"() {
        given: "A country with only 1 resident"
            Person p = new Person(firstName: "Fred", lastName: "Flinstone")
            Country c = new Country(name:"Dinoville")
                            .addToResidents(p)
                            .save(flush:true)

            new Country(name:"Springfield")
                            .addToResidents(firstName:"Homer", lastName:"Simpson")
                            .addToResidents(firstName:"Bart", lastName:"Simpson")
                            .addToResidents(firstName:"Marge", lastName:"Simpson")
                            .save(flush:true)

            session.clear()

        when:"We query for countries with 1 resident"
            def results = Country.withCriteria {
                sizeEq "residents", 1
            }

        then:"We get the correct result back"
            results != null
            results.size() == 1
            results[0].name == 'Dinoville'

        when:"We query for countries with 3 resident"
            results = Country.withCriteria {
                sizeEq "residents", 3
            }

        then:"We get the correct result back"
             results != null
             results.size() == 1
             results[0].name == 'Springfield'


        when:"We query for countries with 2 residents"
            results = Country.withCriteria {
                sizeEq "residents", 2
            }

        then:"we get no results back"
            results.size() == 0

    }
}
