package org.codehaus.groovy.grails.orm.hibernate

import org.junit.Test

import static junit.framework.Assert.*

/**
 * Tests that finders like Foo.findByFooOrBar(x,y) work.
 */
class DynamicFinderOrTests extends AbstractGrailsHibernateTests {

    protected getDomainClasses() {
        [Person, Pet, Face, Nose, DynamicFinderOrBook]
    }

    @Test
    void testFindAllByOr() {
        def b = DynamicFinderOrBook.newInstance()
        b.title = "Groovy in Action"
        b.publisher = "Manning"
        b.save(flush:true)

        def b2 = DynamicFinderOrBook.newInstance()
        b2.title = "Ajax in Action"
        b2.publisher = "Manning"
        b2.save(flush:true)

        assertEquals 1, DynamicFinderOrBook.findAllByTitleAndPublisher("Groovy in Action", "Manning").size()
        assertEquals 1, DynamicFinderOrBook.findAllByTitleAndPublisher("Ajax in Action", "Manning").size()
        assertEquals 2, DynamicFinderOrBook.findAllByTitleOrPublisher("Groovy in Action", "Manning").size()
    }

    @Test
    void testCountByOr() {
        def bookClass = ga.getDomainClass(DynamicFinderOrBook.name)

        def b = bookClass.newInstance()
        b.title = "Groovy in Action"
        b.publisher = "Manning"
        b.save(flush:true)

        def b2 = bookClass.newInstance()
        b2.title = "Ajax in Action"
        b2.publisher = "Manning"
        b2.save(flush:true)

        assertEquals 1, bookClass.clazz.countByTitleAndPublisher("Groovy in Action", "Manning")
        assertEquals 1, bookClass.clazz.countByTitleAndPublisher("Ajax in Action", "Manning")
        assertEquals 2, bookClass.clazz.countByTitleOrPublisher("Groovy in Action", "Manning")
    }

    @Test
    void testCountByWithMultipleOr() {
        new Person(firstName: 'Jake', lastName: 'Brown', age: 11).save()
        new Person(firstName: 'Jeff', lastName: 'Brown', age: 41).save()
        new Person(firstName: 'Zack', lastName: 'Galifianakis', age: 41).save()
        new Person(firstName: 'Zack', lastName: 'Brown', age: 14).save()

        assertEquals 0, Person.countByFirstNameOrLastNameOrAge('Geddy', 'Lee', 99)
        assertEquals 2, Person.countByFirstNameOrLastNameOrAge('Geddy', 'Lee', 41)
        assertEquals 3, Person.countByFirstNameOrLastNameOrAgeGreaterThan('Geddy', 'Lee', 12)
        assertEquals 3, Person.countByFirstNameInListOrLastNameOrAge(['Geddy', 'Alex', 'Zack'], 'Lee', 11)
    }


}
class DynamicFinderOrBook {
    Long id
    Long version
    String title
    String publisher
}
