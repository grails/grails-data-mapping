package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity
import org.codehaus.groovy.grails.commons.*

import static junit.framework.Assert.*
import org.junit.Test


class ManyToManyTests extends AbstractGrailsHibernateTests {

    @Test
    void testManyToManyDomain() {
        def authorDomain = ga.getDomainClass(M2MAuthor.name)
        def bookDomain = ga.getDomainClass(M2MBook.name)

        def books = authorDomain.getPropertyByName("books")
        def authors = bookDomain.getPropertyByName("authors")

        assertTrue books.isManyToMany()
        assertTrue authors.isManyToMany()
        assertFalse books.isOneToMany()
        assertFalse authors.isOneToMany()
    }

    @Test
    void testManyToManyMapping() {
        def a = M2MAuthor.newInstance(name:"Stephen King")
        a.addToBooks(M2MBook.newInstance(title:"The Shining"))
         .addToBooks(M2MBook.newInstance(title:"The Stand"))
         .save(true)
        assertEquals 2, M2MBook.list().size()

        def b = M2MBook.get(1)
        assertNotNull b
        assertNotNull b.authors
        assertEquals 1, b.authors.size()

        a = M2MAuthor.get(1)
        assertNotNull a
        assertNotNull a.books
        assertEquals 2, a.books.size()

        assertEquals b, a.books.find { it.id == 1}
        session.flush()
        session.clear()

        a = M2MAuthor.get(1)
        assertNotNull a
        assert a.books

        b = M2MBook.get(1)
        assertNotNull b
        assert b.authors

        // now let's test the database state
        def c = session.connection()

        def ps = c.prepareStatement("select * from m2mauthor_books")
        def rs = ps.executeQuery()
        assertTrue rs.next()

        assertEquals 1,rs.getInt("m2mauthor_id")
        assertEquals 1,rs.getInt("m2mbook_id")

        assertTrue rs.next()
        assertEquals 1,rs.getInt("m2mauthor_id")
        assertEquals 2,rs.getInt("m2mbook_id")
    }

    @Test
    void testMappedManyToMany() {
        def a = MappedM2mAuthor.newInstance(name:"Stephen King")
        a.addToBooks(MappedM2mBook.newInstance(title:"The Shining"))
         .addToBooks(MappedM2mBook.newInstance(title:"The Stand"))
         .save(true)
        assertEquals 2, MappedM2mBook.list().size()

        def b = MappedM2mBook.get(1)
        assertNotNull b
        assertNotNull b.authors
        assertEquals 1, b.authors.size()

        a = MappedM2mAuthor.get(1)
        assertNotNull a
        assertNotNull a.books
        assertEquals 2, a.books.size()

        assertEquals b, a.books.find { it.id == 1}
        session.flush()
        session.clear()

        a = MappedM2mAuthor.get(1)
        assertNotNull a
        assert a.books

        b = MappedM2mBook.get(1)
        assertNotNull b
        assert b.authors

        // now let's test the database state
        def c = session.connection()

        def ps = c.prepareStatement("select * from mm_author_books")
        def rs = ps.executeQuery()
        assertTrue rs.next()

        assertEquals 1,rs.getInt("mm_author_id")
        assertEquals 1,rs.getInt("mm_book_id")

        assertTrue rs.next()
        assertEquals 1,rs.getInt("mm_author_id")
        assertEquals 2,rs.getInt("mm_book_id")
    }

    @Override
    protected getDomainClasses() {
        [M2MBook, M2MAuthor, MappedM2mAuthor, MappedM2mBook]
    }
}

@Entity
class M2MBook {
    Long id
    Long version

    String title
    static belongsTo = M2MAuthor
    Set authors
    static hasMany = [authors:M2MAuthor]
}

@Entity
class M2MAuthor {
    Long id
    Long version

    String name
    Set books
    static hasMany = [books:M2MBook]
}

@Entity
class MappedM2mBook {
    Long id
    Long version

    String title
    static belongsTo = MappedM2mAuthor
    Set authors
    static hasMany = [authors:MappedM2mAuthor]

    static mapping = {
        authors joinTable:[name:"mm_author_books", key:'mm_book_id' ]
    }
}

@Entity
class MappedM2mAuthor {
    Long id
    Long version

    String name

    Set books
    static hasMany = [books:MappedM2mBook]

    static mapping = {
        books joinTable:[name:"mm_author_books", key:'mm_author_id']
    }
}

