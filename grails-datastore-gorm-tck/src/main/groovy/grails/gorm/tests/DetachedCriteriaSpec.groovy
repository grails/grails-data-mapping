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


    void "Test list method with property projection"() {
        given:"A bunch of people"
            createPeople()

        when:"A detached criteria instance is created that uses a property projection"
            def criteria = new DetachedCriteria(Person)
            criteria.with {
                eq 'lastName', 'Simpson'
            }
            criteria = criteria.property("firstName")

            def results = criteria.list(max: 2).sort()
        then:"The list method returns the right results"
            results.size() == 2
            results == ["Homer", "Marge"]

        when:"A detached criteria instance is created that uses a property projection using property missing"
            criteria = new DetachedCriteria(Person)
            criteria.with {
                eq 'lastName', 'Simpson'
            }
            criteria = criteria.firstName

            results = criteria.list(max: 2).sort()
        then:"The list method returns the right results"
            results.size() == 2
            results == ["Homer", "Marge"]

    }
//
//    void "Test exists method"() {
//        given:"A bunch of people"
//            createPeople()
//
//
//        when:"A detached criteria instance is created matching the last name"
//            def criteria = new DetachedCriteria(Person)
//            criteria.with {
//                eq 'lastName', 'Simpson'
//            }
//
//        then:"The count method returns the right results"
//            criteria.exists() == true
//    }
//    void "Test updateAll method"() {
//        given:"A bunch of people"
//            createPeople()
//
//        when:"A detached criteria is created that deletes all matching records"
//            def criteria = new DetachedCriteria(Person).build {
//                eq 'lastName', 'Simpson'
//            }
//            int total = criteria.updateAll(lastName:"Bloggs")
//
//
//        then:"The number of deletions is correct"
//            total == 4
//            Person.count() == 6
//            criteria.count() == 0
//            Person.countByLastName("Bloggs") == 4
//    }
//
//    void "Test deleteAll method"() {
//        given:"A bunch of people"
//            createPeople()
//
//        when:"A detached criteria is created that deletes all matching records"
//            def criteria = new DetachedCriteria(Person).build {
//                eq 'lastName', 'Simpson'
//            }
//            int total = criteria.deleteAll()
//
//
//        then:"The number of deletions is correct"
//            total == 4
//            Person.count() == 2
//    }
//
//    void "Test iterate of detached criteria"() {
//        given:"A bunch of people"
//            createPeople()
//
//        when:"A detached criteria is created that matches the last name and then iterated over"
//            def criteria = new DetachedCriteria(Person).build {
//                eq 'lastName', 'Simpson'
//            }
//            int total = 0
//            criteria.each {
//                total++
//            }
//
//        then:"The number of iterations is correct"
//            total == 4
//    }
//    void "Test dynamic finder on detached criteria"() {
//        given:"A bunch of people"
//            createPeople()
//
//
//        when:"A detached criteria instance is created matching the last name"
//            def criteria = new DetachedCriteria(Person)
//            criteria.with {
//                eq 'lastName', 'Simpson'
//            }
//
//            def result = criteria.findByFirstNameLike("B%")
//
//        then:"The list method returns the right results"
//            result != null
//            result.firstName == "Bart"
//    }
//
//    void "Test get method on detached criteria and additional criteria"() {
//        given:"A bunch of people"
//            createPeople()
//
//        when:"A detached criteria instance is created matching the last name"
//            def criteria = new DetachedCriteria(Person)
//            criteria.with {
//                eq 'lastName', 'Simpson'
//            }
//
//            def result = criteria.get {
//                like 'firstName', 'B%'
//            }
//        then:"The list method returns the right results"
//            result != null
//            result.firstName == "Bart"
//    }
//
//    void "Test list method on detached criteria and additional criteria"() {
//        given:"A bunch of people"
//            createPeople()
//
//
//        when:"A detached criteria instance is created matching the last name"
//            def criteria = new DetachedCriteria(Person)
//            criteria.with {
//                eq 'lastName', 'Simpson'
//            }
//
//            def results = criteria.list {
//                like 'firstName', 'B%'
//            }
//        then:"The list method returns the right results"
//            results.size() == 1
//            results[0].firstName == "Bart"
//
//        when:"The original detached criteria is queried"
//            results = criteria.list()
//
//        then:"The additional criteria didn't modify the original instance and the correct results are returned"
//            results.size() == 4
//            results.every { it.lastName == 'Simpson'}
//    }
//
//    void "Test count method on detached criteria and additional criteria"() {
//        given:"A bunch of people"
//            createPeople()
//
//
//        when:"A detached criteria instance is created matching the last name and count is called with additional criteria"
//            def criteria = new DetachedCriteria(Person)
//            criteria.with {
//                eq 'lastName', 'Simpson'
//            }
//
//            def result = criteria.count {
//                 like 'firstName', 'B%'
//            }
//        then:"The count method returns the right results"
//            result == 1
//
//    }
//
//    void "Test count method on detached criteria"() {
//        given:"A bunch of people"
//            createPeople()
//
//
//        when:"A detached criteria instance is created matching the last name"
//            def criteria = new DetachedCriteria(Person)
//            criteria.with {
//                eq 'lastName', 'Simpson'
//            }
//
//            def result = criteria.count()
//        then:"The count method returns the right results"
//            result == 4
//
//    }
//    void "Test list method on detached criteria"() {
//        given:"A bunch of people"
//            createPeople()
//
//
//        when:"A detached criteria instance is created matching the last name"
//            def criteria = new DetachedCriteria(Person)
//            criteria.with {
//                eq 'lastName', 'Simpson'
//            }
//
//            def results = criteria.list()
//        then:"The list method returns the right results"
//            results.size() == 4
//            results.every { it.lastName == 'Simpson'}
//    }
//
//    void "Test list method on detached criteria with pagination"() {
//        given:"A bunch of people"
//            createPeople()
//
//        when:"A detached criteria instance is created matching the last name"
//            def criteria = new DetachedCriteria(Person)
//            criteria.build {
//                eq 'lastName', 'Simpson'
//            }
//
//            def results = criteria.list(max: 2)
//        then:"The list method returns the right results"
//            results.size() == 2
//            results.every { it.lastName == 'Simpson'}
//    }


    protected def createPeople() {
        new Person(firstName: "Homer", lastName: "Simpson").save()
        new Person(firstName: "Marge", lastName: "Simpson").save()
        new Person(firstName: "Bart", lastName: "Simpson").save()
        new Person(firstName: "Lisa", lastName: "Simpson").save()
        new Person(firstName: "Barney", lastName: "Rubble").save()
        new Person(firstName: "Fred", lastName: "Flinstone").save()
    }

}
