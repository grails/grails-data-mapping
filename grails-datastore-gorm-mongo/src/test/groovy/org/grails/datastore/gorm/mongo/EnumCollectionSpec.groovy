package org.grails.datastore.gorm.mongo

import grails.gorm.tests.GormDatastoreSpec
import grails.persistence.Entity

class EnumCollectionSpec extends GormDatastoreSpec {

    static {
        GormDatastoreSpec.TEST_CLASSES << Teacher << Teacher2
    }

    void "Test persistence of enum"() {
        given:
            def i = new Teacher(name:"Melvin", subject: Subject.MATH)

            i.save(flush:true)
            session.clear()

        when:
            i = Teacher.findByName("Melvin")

        then:
            i != null
            i.name == 'Melvin'
            i.subject == Subject.MATH
    }

    void "Test persistence of enum collections"() {
        given:
            def i = new Teacher2(name:"Melvin", subject: Subject.MATH)
            i.otherSubjects = [Subject.HISTORY, Subject.HOME_EC]
            i.save(flush:true)
            session.clear()

        when:
            i = Teacher2.findByName("Melvin")

        then:
            i != null
            i.name == 'Melvin'
            i.subject == Subject.MATH
            i.otherSubjects != null
            i.otherSubjects.size() == 2
            i.otherSubjects[0] == Subject.HISTORY
            i.otherSubjects[1] == Subject.HOME_EC
    }
}

@Entity
class Teacher {
    Long id
    String name
    Subject subject

    static mapping = {
        name index:true
    }
}

@Entity
class Teacher2 {
    Long id
    String name
    Subject subject
    List<Subject> otherSubjects

    static mapping = {
        name index:true
    }
}

enum Subject {
	HISTORY, MATH, ENGLISH, HOME_EC;
}
