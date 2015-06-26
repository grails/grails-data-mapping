package grails.test.mixin.hibernate

import grails.test.mixin.TestMixin
import grails.test.mixin.gorm.Domain

@Domain(Person)
@TestMixin(HibernateTestMixin)
public class Junit3BaseclassTest extends GroovyTestCase {
    void testThatGormIsAvailableInParentClass() {
        assert Person.count() >= 0
    }
}
