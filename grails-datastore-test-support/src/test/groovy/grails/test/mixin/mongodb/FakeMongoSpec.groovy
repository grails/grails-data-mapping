package grails.test.mixin.mongodb

import grails.test.mixin.TestMixin
import spock.lang.Ignore
import spock.lang.Specification

import com.github.fakemongo.Fongo


@TestMixin(MongoDbTestMixin)
class FakeMongoSpec extends Specification{

    @Ignore
    void "Test that it is possible to use a Mongo mixin to test MongoDB interaction with Fongo"() {
        given:"A mongo domain model"
            Fongo fongo = new Fongo("mongo server 1")
            mongoDomain(fongo.mongo, [Person])

        expect:"Dynamic finders to work"
            Person.list() != null
            Person.count() != null
    }
}
