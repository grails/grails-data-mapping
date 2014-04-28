package grails.gorm.tests

import grails.gorm.DetachedCriteria
import org.grails.datastore.gorm.query.transform.ApplyDetachedCriteriaTransform

/**
 * Tests for using subqueries in criteria and where method calls
 *
 */
@ApplyDetachedCriteriaTransform
class SubquerySpec extends GormDatastoreSpec {

    def "Test subquery with projection and criteria with closure"() {
        given:"A bunch of people"
        createPeople()

        when:"We query for people above a certain age average"
        def results = Person.withCriteria {
            gt "age",  {
                projections {
                    avg "age"
                }
            }

            order "firstName"
        }

        then:"the correct results are returned"
        results.size() == 4
        results[0].firstName == "Barney"
        results[1].firstName == "Fred"
        results[2].firstName == "Homer"
        results[3].firstName == "Marge"
    }

    def "Test subquery with projection and criteria"() {
        given:"A bunch of people"
        createPeople()

        when:"We query for people above a certain age average"
        def results = Person.withCriteria {
            gt "age", new DetachedCriteria(Person).build {
                projections {
                    avg "age"
                }
            }

            order "firstName"
        }

        then:"the correct results are returned"
        results.size() == 4
        results[0].firstName == "Barney"
        results[1].firstName == "Fred"
        results[2].firstName == "Homer"
        results[3].firstName == "Marge"
    }

    def "Test subquery that uses gtSome"() {
        given:"A bunch of people"
            createPeople()

        when:"We query for people above a certain age average"
            def results = Person.withCriteria {
                gtSome "age", Person.where { age < 18 }.property('age')
                order "firstName"
            }

        then:"the correct results are returned"
            results.size() == 1
            results[0].firstName == "Bart"
    }

    def "Test subquery that uses in"() {
        given:"A bunch of people"
            createPeople()

        when:"We query for people above a certain age average"
//              def results = new GroovyShell().evaluate('''
//import grails.gorm.tests.*
//import org.grails.datastore.gorm.query.transform.ApplyDetachedCriteriaTransform
//
//@ApplyDetachedCriteriaTransform
//class MyQuery {
//    static execute() {
//        Person.where {
//            firstName in where { age < 18 }.property('firstName')
//        }.list()
//    }
//}
//MyQuery.execute()
//''')
            def results = Person.where {
                firstName in where { age < 18 }.property('firstName')
            }.order('firstName').list()


        then:"the correct results are returned"
            results.size() == 2
            results[0].firstName == "Bart"
            results[1].firstName == "Lisa"
    }

    def "Test subquery that uses not in"() {
        given:"A bunch of people"
        createPeople()

        when:"We query for people not in a list of values using a subquery"

            def results = Person.withCriteria {
                notIn "firstName", Person.where { age < 18 }.property('firstName')
                order "firstName"
            }


        then:"the correct results are returned"
        results.size() == 4
    }

    def "Test subquery that exists query"() {
        given:"A bunch of people"
            createPeople()

        when:"We query that uses an exists subquery"

            def results = Person.withCriteria {
                existsFor Person.where { age < 18 }.property('firstName')
                order "firstName"
            }


        then:"the correct results are returned"
            results.size() == 6
    }


    def "Test subquery inside another where method"() {
        given:"A bunch of people"
            createPeople()

        when:"We query for people above a certain age average"
            def results = Person.where {
                age > where { age > 18 }.avg('age')
            }
            .order('firstName')
            .list()

        then:"the correct results are returned"
            results.size() == 2
            results[0].firstName == "Fred"
            results[1].firstName == "Homer"
    }


    protected def createPeople() {
        new Person(firstName: "Homer", lastName: "Simpson", age:45).save()
        new Person(firstName: "Marge", lastName: "Simpson", age:40).save()
        new Person(firstName: "Bart", lastName: "Simpson", age:9).save()
        new Person(firstName: "Lisa", lastName: "Simpson", age:7).save()
        new Person(firstName: "Barney", lastName: "Rubble", age:35).save()
        new Person(firstName: "Fred", lastName: "Flinstone", age:41).save()
    }
}
