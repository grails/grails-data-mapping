package org.grails.datastore.gorm

import grails.async.Promise
import grails.gorm.DetachedCriteria
import grails.gorm.tests.GormDatastoreSpec
import grails.gorm.tests.Person

/**
 */
class DetachedCriteriaAsPromiseSpec extends GormDatastoreSpec{

    def "Test a DetachedCriteria instance can be turned into a Promise"()  {
        given:"Some people"
            new Person(firstName: "Homer", lastName: "Simpson").save()
            new Person(firstName: "Bart", lastName: "Simpson").save()
            new Person(firstName: "Barney", lastName: "Rubble").save(flush:true)
            session.clear()

        when:"We cast a DetachedCriteria to a Promise"
            DetachedCriteria query = Person.where( {
                lastName == "Simpson"
            })

            def promise = query as Promise

        then:"A promise is returned"
            promise instanceof Promise

        when:"The promise value is returned"
            def results = promise.get()

        then:"They are correct"
            results.size() == 2

    }

    def "Test a DetachedCriteria instance with the async namespace"()  {
        given:"Some people"
        new Person(firstName: "Homer", lastName: "Simpson").save()
        new Person(firstName: "Bart", lastName: "Simpson").save()
        new Person(firstName: "Barney", lastName: "Rubble").save(flush:true)
        session.clear()

        when:"We cast a DetachedCriteria to a Promise"
        DetachedCriteria query = Person.where( {
            lastName == "Simpson"
        })

            def promise = query.async.list()

        then:"A promise is returned"
            promise instanceof Promise

        when:"The promise value is returned"
            def results = promise.get()

        then:"They are correct"
            results.size() == 2

        when:"A dynamic finder is used async"
            Person p = query.async.findByFirstName("Homer").get()

        then:"The result is correct"
            p != null
            p.firstName == "Homer"

    }
}
