package grails.gorm.tests

import grails.gorm.PagedResultList

import spock.lang.Ignore

/**
 * Ignored for DynamoDB because DynamoDB doesn't support pagination.
 */
@Ignore
class PagedResultSpec extends GormDatastoreSpec {

    void "Test that a paged result list is returned from the list() method with pagination params"() {
        given:"Some people"
            createPeople()

        when:"The list method is used with pagination params"
            def results = Person.list(offset:2, max:2)

        then:"You get a paged result list back"
            results instanceof PagedResultList
            results.size() == 2
            results[0].firstName == "Bart"
            results[1].firstName == "Lisa"
            results.totalCount == 6
    }

    void "Test that a paged result list is returned from the critera with pagination params"() {
        given:"Some people"
            createPeople()

        when:"The list method is used with pagination params"
            def results = Person.createCriteria().list(offset:1, max:2) {
                eq 'lastName', 'Simpson'
            }

        then:"You get a paged result list back"
            results instanceof PagedResultList
            results.size() == 2
            results[0].firstName == "Marge"
            results[1].firstName == "Bart"
            results.totalCount == 4
    }

    protected void createPeople() {
        new Person(firstName: "Homer", lastName: "Simpson", age:45).save()
        new Person(firstName: "Marge", lastName: "Simpson", age:40).save()
        new Person(firstName: "Bart", lastName: "Simpson", age:9).save()
        new Person(firstName: "Lisa", lastName: "Simpson", age:7).save()
        new Person(firstName: "Barney", lastName: "Rubble", age:35).save()
        new Person(firstName: "Fred", lastName: "Flinstone", age:41).save()
    }
}
