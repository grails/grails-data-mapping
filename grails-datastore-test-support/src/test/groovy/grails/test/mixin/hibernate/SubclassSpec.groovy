package grails.test.mixin.hibernate

import spock.lang.Issue

class SubclassSpec extends SpecBase {

    def setup() {
    }

    def cleanup() {
    }

    @Issue("GRAILS-11573")
    void "Test that it is possible to use a Hibernate mixin that's defined in super class"() {
        expect:
        Person.count() == 0
    }
}
