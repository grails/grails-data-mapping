package org.grails.datastore.gorm

import grails.gorm.annotation.Entity
import grails.gorm.tests.GormDatastoreSpec

class GormDirtyCheckingSpec extends GormDatastoreSpec {

    @Override
    List getDomainClasses() {
        [Student]
    }

    void "test a new instance is dirty by default"() {

        when:
        Student student = new Student(name: "JD")

        then:
        student.isDirty()
    }

}

@Entity
class Student {
    String name
}
