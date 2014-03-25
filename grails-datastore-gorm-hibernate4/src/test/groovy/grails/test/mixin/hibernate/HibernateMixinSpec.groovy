package grails.test.mixin.hibernate

import grails.persistence.Entity
import grails.test.mixin.TestMixin
import spock.lang.Specification

/**
 * Created by graemerocher on 24/03/14.
 */
@TestMixin(HibernateTestMixin)
class HibernateMixinSpec extends Specification{

    void setupSpec() {
        hibernateDomain([Person])
    }
    void "Test that it is possible to use a Hibernate mixin to test Hibernate interaction"() {
        expect:"Dynamic finders to work"
            Person.count() == 0
            sessionFactory != null
            transactionManager != null
            session != null
    }
}

@Entity
class Person {
    Long id
    Long version
    String name
}

