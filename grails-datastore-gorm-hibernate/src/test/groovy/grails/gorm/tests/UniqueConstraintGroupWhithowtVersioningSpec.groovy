package grails.gorm.tests

import grails.persistence.Entity
import spock.lang.Issue

class UniqueConstraintGroupWithoutVersioningSpec extends GormDatastoreSpec{

    @Issue('GRAILS-9936')
    void "Test transient uniqueness handling"() {
        given:"Some linked objects"
            def a2 = new UniqueConstraintTestParentEntity(prop: "prop1")
            a2.id = 1
            def b3 = new UniqueConstraintTestChildEntity(prop: "b1", a: a2)
            a2.bs = [b3]
        when:"validate child item"
            def processed = true
            try {
                b3.validate()
            } catch (Exception ex) {
                processed = false
            }
        then:"no exception is thrown"
            processed
    }

    @Override
    List getDomainClasses() {
        [UniqueConstraintTestParentEntity, UniqueConstraintTestChildEntity]
    }

    def setup() {}
}

@Entity
class UniqueConstraintTestParentEntity {
    Long id
    String prop
    static hasMany = [bs: UniqueConstraintTestChildEntity]
    static mapping = {
        version false
        id generator: "assigned"
    }
}

@Entity
class UniqueConstraintTestChildEntity {
    String prop
    static belongsTo = [a: UniqueConstraintTestParentEntity]

    static constraints = {
        prop unique:["a"]
    }
}
