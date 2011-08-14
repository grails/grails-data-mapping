package grails.gorm.tests

import grails.gorm.JpaEntity

import javax.persistence.EnumType
import javax.persistence.Enumerated

//@JpaEntity
class EnumThing {
    String name

    @Enumerated(EnumType.STRING)
    TestEnum en
}
