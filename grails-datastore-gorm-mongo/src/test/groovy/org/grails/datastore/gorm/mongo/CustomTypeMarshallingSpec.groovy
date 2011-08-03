package org.grails.datastore.gorm.mongo

import grails.gorm.tests.GormDatastoreSpec
import org.bson.types.ObjectId

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 8/3/11
 * Time: 4:22 PM
 * To change this template use File | Settings | File Templates.
 */
class CustomTypeMarshallingSpec extends GormDatastoreSpec{
    static {
        TEST_CLASSES << Person
    }

    void "Test basic crud with custom types"() {
        given: "A custom type registered for the Birthday class"
            final now = new Date()
            def p = new Person(name:"Fred", birthday: new Birthday(now))
            p.save(flush:true)
            session.clear()

        when: "We query the person"
            p = Person.findByName("Fred")

        then: "The birthday is returned"
            p != null
            p.name == "Fred"
            p.birthday != null

        when: "We query with a custom type"
           p = Person.findByBirthday(new Birthday(now))

        then:
            p != null
    }

}

class Person {
    ObjectId id
    String name
    Birthday birthday
}
class Birthday {
    Date date

    Birthday(Date date) {
        this.date = date
    }
}
