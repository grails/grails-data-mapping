package grails.test.mixin.hibernate

import grails.test.mixin.TestMixin
import grails.test.mixin.gorm.Domain
import spock.lang.Specification


@Domain(Person)
@TestMixin(HibernateTestMixin)
class DomainAnnotationSpec extends Specification{
    void "should allow registering domains with Domain annotation"() {
        given:
            def person = new Person(name:'John Doe')
            def personId = person.save(flush:true, failOnError:true)?.id
        expect:"Dynamic finders to work"
            Person.count() == 1
            Person.get(personId).name == 'John Doe'
            sessionFactory != null
            transactionManager != null
            hibernateSession != null
    }
}
