package grails.gorm.tests

import grails.gorm.JpaEntity
import javax.persistence.Transient

@JpaEntity
class ClassWithOverloadedBeforeValidate {
    Long id
    Long version

    @Transient
    def noArgCounter = 0

    @Transient
    def listArgCounter = 0

    @Transient
    def propertiesPassedToBeforeValidate

    String name
    def beforeValidate() {
        ++noArgCounter
    }
    def beforeValidate(List properties) {
        ++listArgCounter
        propertiesPassedToBeforeValidate = properties
    }

    static constraints = {
        name blank: false
    }
}