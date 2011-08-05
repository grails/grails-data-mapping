package org.grails.datastore.gorm

import grails.gorm.tests.GormDatastoreSpec

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 8/3/11
 * Time: 11:03 PM
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

        when: "A range query is executed"

            p = Person.findByBirthdayBetween(new Birthday(now-1), new Birthday(now+1))
            def p2 = Person.findByBirthdayBetween(new Birthday(now+1), new Birthday(now+2))

        then:
            p != null
            p2 == null
    }

}

class Person {
    Long id
    String name
    Birthday birthday
}
class Birthday implements Comparable{
    Date date

    Birthday(Date date) {
        this.date = date
    }

    @Override
    int compareTo(Object t) {
        date.compareTo(t.date)
    }
}
