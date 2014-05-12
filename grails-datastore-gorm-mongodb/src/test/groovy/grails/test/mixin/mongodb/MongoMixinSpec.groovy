package grails.test.mixin.mongodb

import grails.persistence.Entity
import grails.test.mixin.TestMixin
import spock.lang.Specification

/**
 * Created by graemerocher on 24/03/14.
 */
@TestMixin(MongoDbTestMixin)
class MongoMixinSpec extends Specification{

    void "Test that it is possible to use a Mongo mixin to test MongoDB interaction"() {
        given:"A mongo domain model"
            mongoDomain([Person])

        expect:"Dynamic finders to work"
            Person.list() != null
            Person.count() != null
    }
}

@Entity
class Person {
    Object id
    Long version
    String name
}
