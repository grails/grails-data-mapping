package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

import static junit.framework.Assert.*
import org.junit.Test

/**
 * Test for GRAILS-3911
 */
class SortWithNestedPropertiesTests extends AbstractGrailsHibernateTests {

    def bookClass

    protected getDomainClasses() {
        [SortBook, SortAuthor, SortPerson]
    }

    @Override
    void onSetUp() {

        def personClass = ga.getDomainClass(SortPerson.name).clazz
        def authorClass = ga.getDomainClass(SortAuthor.name).clazz
        bookClass = ga.getDomainClass(SortBook.name).clazz
        def addressClass = ga.classLoader.loadClass("org.codehaus.groovy.grails.orm.hibernate.SortAddress")
        ['C','A','b','a','c','B'].eachWithIndex { name, i ->
            def person = personClass.newInstance(version:1, name:name).save(flush:true)
            def author = authorClass.newInstance(version:1, name:name, person:person).save(flush:true)
            def address = addressClass.newInstance(street:name, city:'Oslo')
            bookClass.newInstance(version:1, title:'foo', author:author, address:address).save(flush:true)
        }
    }

    @Test
    void testListPersistentMethod() {
        assertEquals(['A','a','b','B','C','c'], bookClass.list(sort:'author.name').author.name)
        assertEquals(['A','B','C','a','b','c'], bookClass.list(sort:'author.name', ignoreCase:false).author.name)
    }

    @Test
    void testHibernateNamedQueriesBuilder() {
        assertEqualsIgnoreCase(['A','a','b','B','C','c'], bookClass.manningBooks().list(sort:'author.name').author.name)
    }

    @Test
    void testFindAllWherePersistentMethod() {
        assertEquals(['A','B','C', 'a','b','c'], bookClass.findAllWhere([publisher: 'Manning']).author.name.sort())
    }

    @Test
    void testFindAllByPersistentMethod() {
        assertEqualsIgnoreCase(['A','a','b','B','C','c'], bookClass.findAllByPublisher('Manning', [sort:'author.name']).author.name)
    }

    @Test
    void testFindByPersistentMethod() {
        assertEquals('A', bookClass.findByPublisher('Manning', [sort:'author.name']).author.name)
    }

    @Test
    void testDeepSort() {
        assertEqualsIgnoreCase(['A','a','b','B','C','c'], bookClass.list(sort:'author.person.name').author.person.name)
    }

    @Test
    void testPreserveOtherParameters() {
        assertEqualsIgnoreCase(['b','B','C'], bookClass.list(max:3, offset:2, sort:'author.name').author.name)
        assertEquals(['C','a','b'], bookClass.list(max:3, offset:2, sort:'author.name', ignoreCase:false).author.name)
        assertEqualsIgnoreCase(['b','B','C'], bookClass.manningBooks().list(max:3, offset:2, sort:'author.name').author.name)
        assertEqualsIgnoreCase(['b','B','C'], bookClass.findAllByPublisher('Manning', [max:3, offset:2, sort:'author.name']).author.name)
        assertEqualsIgnoreCase(['b','B','C'], bookClass.list(max:3, offset:2, sort:'author.person.name').author.person.name)
    }

    void assertEqualsIgnoreCase(a, b) {
        assertEquals(a.collect{it.toLowerCase()}, b.collect{it.toLowerCase()})
    }

    @Test
    void testSortByEmbeddedProperty() {
        assertEqualsIgnoreCase(['A','a','b','B','C','c'], bookClass.list(sort:'address.street').address.street)
    }

    @Test
    void testDefaultSort() {
        assertEqualsIgnoreCase(['A','a','b','B','C','c'], bookClass.list().address.street)
    }
}

@Entity
class SortBook {
    Long id
    Long version

    String title
    SortAuthor author
    SortAddress address
    String publisher = 'Manning'
    static embedded = ["address"]
    static namedQueries = {
        manningBooks {
            eq('publisher', 'Manning')
        }
    }
    static mapping = {
        sort 'author.name'
    }
}

@Entity
class SortAuthor {
    Long id
    Long version

    String name
    SortPerson person
}

@Entity
class SortPerson {
    Long id
    Long version

    String name
}

class SortAddress {
    String street
    String city
}

