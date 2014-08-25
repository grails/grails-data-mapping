package grails.gorm.tests

import grails.gorm.DetachedCriteria
import grails.gorm.PagedResultList

class DetachedCriteriaSpec extends GormDatastoreSpec {

    void "Test the list method returns a PagedResultList with pagination arguments"() {
        given:"A bunch of people"
            createPeople()

        when:"A detached criteria instance is created and the list method used with the max parameter"
            def criteria = new DetachedCriteria(PersonLastNamePartitionKey)
            criteria.with {
                eq 'lastName', 'Simpson'
            }
            def results = criteria.list(max:2)

        then:"The results are a PagedResultList"
            results instanceof PagedResultList
            results.totalCount == 4
            results.size() == 2
            results.every { it.lastName == "Simpson" }
        
        
        when:"A detached criteria instance is created and the list method used with the max and offset parameters"
            criteria = new DetachedCriteria(PersonLastNamePartitionKey)
            criteria.with {
                eq 'lastName', 'Simpson'
            }
            results = criteria.list(offset:2, max:4).totalCount

        then:"UnsupportedOperationException is thrown"
            thrown UnsupportedOperationException
        
    }

    /**
     * An entity with generated uuid will return random rows when using max, hence the need to use
     * an entity with assigned primary key with rows selected with cluster key order
     */
    void "Test list method with property projection"() {
        given:"A bunch of people"
            createPeople()

        when:"A detached criteria instance is created that uses a property projection"
            def criteria = new DetachedCriteria(PersonLastNamePartitionKey)
            criteria.with {
                eq 'lastName', 'Simpson'
            }
            criteria = criteria.property("firstName")

            def results = criteria.list(max: 2).sort()
        then:"The list method returns the right results"
            results.size() == 2
            results == ["Bart", "Homer"]

        when:"A detached criteria instance is created that uses a property projection using property missing"
            criteria = new DetachedCriteria(PersonLastNamePartitionKey)
            criteria.with {
                eq 'lastName', 'Simpson'
            }
            criteria = criteria.firstName

            results = criteria.list(max: 2).sort()
        then:"The list method returns the right results"
            results.size() == 2
            results == ["Bart", "Homer"]

    }

    void "Test exists method"() {
        given:"A bunch of people"
            createPeople()


        when:"A detached criteria instance is created matching the last name"
            def criteria = new DetachedCriteria(PersonLastNamePartitionKey)
            criteria.with {
                eq 'lastName', 'Simpson'
            }

        then:"The count method returns the right results"
            criteria.asBoolean() == true
    }
    
    void "Test updateAll method"() {
        given:"A bunch of people"
            createPeople()

        when:"A detached criteria is created that deletes all matching records"
            def criteria = new DetachedCriteria(PersonLastNamePartitionKey).build {
                eq 'location', 'USA'
            }                       
            int total = criteria.updateAll(location: "Springfield")


        then:"The number of deletions is correct"
            PersonLastNamePartitionKey.count() == 6
            criteria.count() == 0
            PersonLastNamePartitionKey.countByLocation("Springfield") == 4
    }

    void "Test deleteAll method"() {
        given:"A bunch of people"
            createPeople()

        when:"A detached criteria is created that deletes all matching records"
            def criteria = new DetachedCriteria(PersonLastNamePartitionKey).build {
                eq 'lastName', 'Simpson'
            }
            int total = criteria.deleteAll()


        then:"The number of deletions is correct"
            PersonLastNamePartitionKey.count() == 2
    }

    void "Test iterate of detached criteria"() {
        given:"A bunch of people"
            createPeople()

        when:"A detached criteria is created that matches the last name and then iterated over"
            def criteria = new DetachedCriteria(PersonLastNamePartitionKey).build {
                eq 'lastName', 'Simpson'
            }
            int total = 0
            criteria.each {
                total++
            }

        then:"The number of iterations is correct"
            total == 4
    }
    void "Test dynamic finder on detached criteria"() {
        given:"A bunch of people"
            createPeople()


        when:"A detached criteria instance is created matching the last name"
            def criteria = new DetachedCriteria(PersonLastNamePartitionKey)
            criteria.with {
                eq 'lastName', 'Simpson'
            }

            def result = criteria.findByFirstName("Bart")

        then:"The list method returns the right results"
            result != null
            result.firstName == "Bart"
    }

    void "Test get method on detached criteria and additional criteria"() {
        given:"A bunch of people"
            createPeople()

        when:"A detached criteria instance is created matching the last name"
            def criteria = new DetachedCriteria(PersonLastNamePartitionKey)
            criteria.with {
                eq 'lastName', 'Simpson'
            }

            def result = criteria.get {
                eq 'firstName', 'Bart'
            }
        then:"The list method returns the right results"
            result != null
            result.firstName == "Bart"
    }

    void "Test list method on detached criteria and additional criteria"() {
        given:"A bunch of people"
            createPeople()


        when:"A detached criteria instance is created matching the last name"
            def criteria = new DetachedCriteria(PersonLastNamePartitionKey)
            criteria.with {
                eq 'lastName', 'Simpson'
            }

            def results = criteria.list {
                eq 'firstName', 'Bart'
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
            createPeople()


        when:"A detached criteria instance is created matching the last name and count is called with additional criteria"
            def criteria = new DetachedCriteria(PersonLastNamePartitionKey)
            criteria.with {
                eq 'lastName', 'Simpson'
            }

            def result = criteria.count {
                 eq 'firstName', 'Bart'
            }
        then:"The count method returns the right results"
            result == 1

    }

    void "Test count method on detached criteria"() {
        given:"A bunch of people"
            createPeople()


        when:"A detached criteria instance is created matching the last name"
            def criteria = new DetachedCriteria(PersonLastNamePartitionKey)
            criteria.with {
                eq 'lastName', 'Simpson'
            }

            def result = criteria.count()
        then:"The count method returns the right results"
            result == 4

    }
    void "Test list method on detached criteria"() {
        given:"A bunch of people"
            createPeople()


        when:"A detached criteria instance is created matching the last name"
            def criteria = new DetachedCriteria(PersonLastNamePartitionKey)
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
            createPeople()

        when:"A detached criteria instance is created matching the last name"
            def criteria = new DetachedCriteria(PersonLastNamePartitionKey).build {
                eq 'lastName', 'Simpson'
            }

            def results = criteria.list(max: 2)
        then:"The list method returns the right results"
            results.size() == 2
            results.every { it.lastName == 'Simpson'}
    }

    protected void createPeople() {
        new PersonLastNamePartitionKey(firstName: "Homer", lastName: "Simpson", location: "USA").save()
        new PersonLastNamePartitionKey(firstName: "Marge", lastName: "Simpson", location: "USA").save()
        new PersonLastNamePartitionKey(firstName: "Bart", lastName: "Simpson", location: "USA").save()
        new PersonLastNamePartitionKey(firstName: "Lisa", lastName: "Simpson", location: "USA").save()
        new PersonLastNamePartitionKey(firstName: "Barney", lastName: "Rubble", location: "Bedrock ").save()
        new PersonLastNamePartitionKey(firstName: "Fred", lastName: "Flinstone", location: "Bedrock ").save()
    }
}
