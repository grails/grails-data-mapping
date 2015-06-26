package grails.test.mixin.hibernate

import grails.test.mixin.TestMixin
import grails.test.mixin.gorm.Domain

@Domain(Person)
@TestMixin(HibernateTestMixin)
class BaseTests {
}