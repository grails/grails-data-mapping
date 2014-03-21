package org.grails.datastore.gorm.mongo

import com.mongodb.MongoException
import grails.gorm.CriteriaBuilder
import grails.gorm.tests.GormDatastoreSpec
import grails.gorm.tests.Person
import org.springframework.data.mongodb.UncategorizedMongoDbException


class HintQueryArgumentSpec extends GormDatastoreSpec {

    void "Test that hints work on criteria queries"() {
        when:"A criteria query is created with a hint"
            CriteriaBuilder c = Person.createCriteria()
            c.list {
                eq 'firstName', 'Bob'
                arguments hint:["firstName":1]
            }

        then:"The query contains the hint"
            c.query.@queryArguments == [hint:['firstName':1]]

        when:"A dynamic finder uses a hint"
            def results = Person.findAllByFirstName("Bob", [hint:"firstName"])
            for(e in results) {} // just to trigger the query

        then:"The hint is used"
            MongoException exception = thrown()
            exception.message == 'bad hint'

        when:"A dynamic finder uses a hint"
            results = Person.findAllByFirstName("Bob", [hint:["firstName":1]])

        then:"The hint is used"
            results.size() == 0
     }
}
