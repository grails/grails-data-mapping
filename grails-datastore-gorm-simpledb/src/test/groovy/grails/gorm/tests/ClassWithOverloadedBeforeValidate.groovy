package grails.gorm.tests

import grails.persistence.Entity

/**
 * Test entity for testing AWS SimpleDB.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */

@Entity
class ClassWithOverloadedBeforeValidate implements Serializable {
    String id
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

    static mapping = {
        domain 'ClassWithOverloadedBeforeValidate'
    }
}