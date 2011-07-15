package grails.gorm.tests

import grails.gorm.JpaEntity
import javax.persistence.Transient

@JpaEntity
class ClassWithNoArgBeforeValidate {
    Long id
    Long version

    @Transient
    def noArgCounter = 0

    String name

    def beforeValidate() {
        ++noArgCounter
    }

    static constraints = {
        name blank: false
    }
}