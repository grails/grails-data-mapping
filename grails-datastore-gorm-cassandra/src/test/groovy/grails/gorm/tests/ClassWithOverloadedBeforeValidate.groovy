package grails.gorm.tests

import grails.gorm.CassandraEntity

@CassandraEntity
class ClassWithOverloadedBeforeValidate implements Serializable {
    UUID id
    Long version
    def noArgCounter = 0
    def listArgCounter = 0
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