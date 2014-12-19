package org.grails.orm.hibernate

import grails.persistence.Entity

import static junit.framework.Assert.*
import org.junit.Test


class ListMappingTests extends AbstractGrailsHibernateTests {

    @Test
    void testAddPersistentPogoToList() {
        def b = ListMappingBook.newInstance()
        def a = ListMappingAuthor.newInstance()

        a.name = "Stephen King"
        a.save()

        b.addToAuthors(a)
        b.save()
        session.flush()
        session.clear()

        b = ListMappingBook.get(1)
        assertEquals "Stephen King",b.authors[0].name
    }

    @Test
    void testListMapping() {
        def a1 = ListMappingAuthor.newInstance()
        def a2 = ListMappingAuthor.newInstance()
        def a3 = ListMappingAuthor.newInstance()

        a1.name = "Stephen King"
        a2.name = "James Patterson"
        a3.name = "Joe Bloggs"

        def book = ListMappingBook.newInstance()
        book.addToAuthors(a1)
            .addToAuthors(a2)
            .addToAuthors(a3)
            .save(true)

        session.flush()
        session.clear()

        def ids = [a1.id, a2.id, a2.id]

        book = ListMappingBook.get(1)
        assertEquals 3, book.authors.size()
        assertEquals a1.id, book.authors[0].id
        assertEquals a2.id, book.authors[1].id
        assertEquals a3.id, book.authors[2].id
    }

    @Override
    protected getDomainClasses() {
        [ListMappingAuthor, ListMappingBook]
    }
}

@Entity
class ListMappingBook {
    Long id
    Long version

    List authors
    static hasMany = [authors:ListMappingAuthor]
}

@Entity
class ListMappingAuthor {
    Long id
    Long version

    String name
    ListMappingBook book
}

