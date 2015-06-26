package grails.test.mixin.hibernate

import grails.test.mixin.TestMixin
import grails.test.mixin.gorm.Domain
import spock.lang.Specification

@Domain(Person)
@TestMixin(HibernateTestMixin)
class SpecBase extends Specification {
}