package grails.gorm.tests

class PagedResultSpec extends GormDatastoreSpec {

    void "Test that a paged result list is returned from the list() method with pagination params"() {
        given:"Some people"
            createPeople()

        when:"The list method is used with pagination params"
            def results = PersonLastNamePartitionKey.list(max:2)

        then:"You get a paged result list back"
            results.getClass().simpleName == 'PagedResultList' // Grails/Hibernate has a custom class in different package
            results.size() == 2
            results[0].firstName == "Barney"
            results[1].firstName == "Fred"
            results.totalCount == 6
       
        when:  
            PersonLastNamePartitionKey.list(offset:1, max:2).size()
        then: //offset not supported by Cassandra
            thrown UnsupportedOperationException
            
    }

    void "Test that a paged result list is returned from the critera with pagination params"() {
        given:"Some people"
            createPeople()

        when:"The list method is used with pagination params"
            def results = PersonLastNamePartitionKey.createCriteria().list(max:2) {
                eq 'lastName', 'Simpson'
            }

        then:"You get a paged result list back"
            results.getClass().simpleName == 'PagedResultList' // Grails/Hibernate has a custom class in different package
            results.size() == 2
            results[0].firstName == "Bart"
            results[1].firstName == "Homer"
            results.totalCount == 4
    }

    protected void createPeople() {
        new PersonLastNamePartitionKey(firstName: "Homer", lastName: "Simpson", age:45).save()
        new PersonLastNamePartitionKey(firstName: "Marge", lastName: "Simpson", age:40).save()
        new PersonLastNamePartitionKey(firstName: "Bart", lastName: "Simpson", age:9).save()
        new PersonLastNamePartitionKey(firstName: "Lisa", lastName: "Simpson", age:7).save()
        new PersonLastNamePartitionKey(firstName: "Barney", lastName: "Rubble", age:35).save()
        new PersonLastNamePartitionKey(firstName: "Fred", lastName: "Flinstone", age:41).save()
    }
}
