package org.grails.datastore.gorm

import grails.async.Promise
import grails.gorm.tests.GormDatastoreSpec
import grails.gorm.tests.Person

/**
 */
class AsyncReadMethodsSpec extends GormDatastoreSpec{

    def "Test that the list method works async"()  {
        given:"Some people"
            new Person(firstName: "Homer", lastName: "Simpson").save()
            new Person(firstName: "Bart", lastName: "Simpson").save()
            new Person(firstName: "Barney", lastName: "Rubble").save(flush:true)
            session.clear()

        when:"We list all entities async"
            def promise = Person.async.list()

        then:"A promise is returned"
            promise instanceof Promise

        when:"The promise value is returned"
            def results = promise.get()

        then:"They are correct"
            results.size() == 3

    }
}
