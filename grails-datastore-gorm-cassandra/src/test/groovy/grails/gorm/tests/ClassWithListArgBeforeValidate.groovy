package grails.gorm.tests

import grails.gorm.CassandraEntity

@CassandraEntity
class ClassWithListArgBeforeValidate implements Serializable {
    UUID id
    Long version
    def listArgCounter = 0
    def propertiesPassedToBeforeValidate
    String name

    def beforeValidate(List properties) {
        ++listArgCounter
        propertiesPassedToBeforeValidate = properties
    }

    static constraints = {
        name blank: false
    }
}