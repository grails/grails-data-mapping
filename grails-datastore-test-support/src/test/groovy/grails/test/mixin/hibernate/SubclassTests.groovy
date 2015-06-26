package grails.test.mixin.hibernate

import static org.junit.Assert.*

import org.junit.Test

import spock.lang.Issue

class SubclassTests extends BaseTests {
    @Issue("GRAILS-11573")
    @Test
    void "Test that it is possible to use a Hibernate mixin that's defined in super class"() {
        assert Person.count() == 0
    }
}
