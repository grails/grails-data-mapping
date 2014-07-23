package grails.gorm.tests

import grails.gorm.CassandraEntity

@CassandraEntity
class ClassWithNoArgBeforeValidate implements Serializable {
    UUID id
    Long version
    def noArgCounter = 0
    String name

    def beforeValidate() {
        ++noArgCounter
    }

    static constraints = {
        name blank: false
    }
}