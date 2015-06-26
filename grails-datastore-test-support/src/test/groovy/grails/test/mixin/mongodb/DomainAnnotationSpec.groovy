package grails.test.mixin.mongodb

import grails.test.mixin.TestMixin
import grails.test.mixin.gorm.Domain
import spock.lang.Specification


@Domain(Person)
@TestMixin(MongoDbTestMixin)
class DomainAnnotationSpec extends Specification{
    void "should allow registering domains with Domain annotation"() {
        given:
            def person = new Person(name:'John Doe')
            def personId = person.save(flush:true, failOnError:true)?.id
        expect:"Dynamic finders to work"
            Person.count() > 0
            Person.get(personId).name == 'John Doe'
    }
}
