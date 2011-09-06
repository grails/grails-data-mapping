package grails.gorm.tests

import grails.gorm.DetachedCriteria

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 9/6/11
 * Time: 12:09 PM
 * To change this template use File | Settings | File Templates.
 */
class DetachedCriteriaSpec extends GormDatastoreSpec{

    void "Test list method on detached criteria and additional criteria"() {
        given:"A bunch of people"
            new Person(firstName:"Homer", lastName: "Simpson").save()
            new Person(firstName:"Marge", lastName: "Simpson").save()
            new Person(firstName:"Bart", lastName: "Simpson").save()
            new Person(firstName:"Lisa", lastName: "Simpson").save()
            new Person(firstName:"Barney", lastName: "Rubble").save()
            new Person(firstName:"Fred", lastName: "Flinstone").save()


        when:"A detached criteria instance is created matching the last name"
            def criteria = new DetachedCriteria(Person)
            criteria.with {
                eq 'lastName', 'Simpson'
            }

            def results = criteria.list {
                like 'firstName', 'B%'
            }
        then:"The list method returns the right results"
            results.size() == 1
            results[0].firstName == "Bart"

        when:"The original detached criteria is queried"
            results = criteria.list()

        then:"The additional criteria didn't modify the original instance and the correct results are returned"
            results.size() == 4
            results.every { it.lastName == 'Simpson'}
    }

    void "Test count method on detached criteria and additional criteria"() {
        given:"A bunch of people"
            new Person(firstName:"Homer", lastName: "Simpson").save()
            new Person(firstName:"Marge", lastName: "Simpson").save()
            new Person(firstName:"Bart", lastName: "Simpson").save()
            new Person(firstName:"Lisa", lastName: "Simpson").save()
            new Person(firstName:"Barney", lastName: "Rubble").save()
            new Person(firstName:"Fred", lastName: "Flinstone").save()


        when:"A detached criteria instance is created matching the last name and count is called with additional criteria"
            def criteria = new DetachedCriteria(Person)
            criteria.with {
                eq 'lastName', 'Simpson'
            }

            def result = criteria.count {
                 like 'firstName', 'B%'
            }
        then:"The count method returns the right results"
            result == 1

    }

    void "Test count method on detached criteria"() {
        given:"A bunch of people"
            new Person(firstName:"Homer", lastName: "Simpson").save()
            new Person(firstName:"Marge", lastName: "Simpson").save()
            new Person(firstName:"Bart", lastName: "Simpson").save()
            new Person(firstName:"Lisa", lastName: "Simpson").save()
            new Person(firstName:"Barney", lastName: "Rubble").save()
            new Person(firstName:"Fred", lastName: "Flinstone").save()


        when:"A detached criteria instance is created matching the last name"
            def criteria = new DetachedCriteria(Person)
            criteria.with {
                eq 'lastName', 'Simpson'
            }

            def result = criteria.count()
        then:"The count method returns the right results"
            result == 4

    }
    void "Test list method on detached criteria"() {
        given:"A bunch of people"
            new Person(firstName:"Homer", lastName: "Simpson").save()
            new Person(firstName:"Marge", lastName: "Simpson").save()
            new Person(firstName:"Bart", lastName: "Simpson").save()
            new Person(firstName:"Lisa", lastName: "Simpson").save()
            new Person(firstName:"Barney", lastName: "Rubble").save()
            new Person(firstName:"Fred", lastName: "Flinstone").save()


        when:"A detached criteria instance is created matching the last name"
            def criteria = new DetachedCriteria(Person)
            criteria.with {
                eq 'lastName', 'Simpson'
            }

            def results = criteria.list()
        then:"The list method returns the right results"
            results.size() == 4
            results.every { it.lastName == 'Simpson'}
    }

    void "Test list method on detached criteria with pagination"() {
        given:"A bunch of people"
            new Person(firstName:"Homer", lastName: "Simpson").save()
            new Person(firstName:"Marge", lastName: "Simpson").save()
            new Person(firstName:"Bart", lastName: "Simpson").save()
            new Person(firstName:"Lisa", lastName: "Simpson").save()
            new Person(firstName:"Barney", lastName: "Rubble").save()
            new Person(firstName:"Fred", lastName: "Flinstone").save()


        when:"A detached criteria instance is created matching the last name"
            def criteria = new DetachedCriteria(Person)
            criteria.build {
                eq 'lastName', 'Simpson'
            }

            def results = criteria.list(max: 2)
        then:"The list method returns the right results"
            results.size() == 2
            results.every { it.lastName == 'Simpson'}
    }
}
