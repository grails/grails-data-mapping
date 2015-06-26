package grails.test.mixin.hibernate

import grails.persistence.Entity
import grails.test.mixin.TestMixin
import spock.lang.Specification
import spock.lang.Stepwise

/**
 * Created by graemerocher on 24/03/14.
 */
@Stepwise
@TestMixin(HibernateTestMixin)
class HibernateMixinSpec extends Specification{

    void setupSpec() {
        hibernateDomain([Person])
    }
    void "Test that it is possible to use a Hibernate mixin to test Hibernate interaction"() {
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

    void "Test that the transaction is rolled back after each test"() {
        expect:
            Person.count() == 0
    }
}

@Entity
class Person {
    Long id
    Long version
    String name
    
    static mapping = {
        cache true
    }
}

