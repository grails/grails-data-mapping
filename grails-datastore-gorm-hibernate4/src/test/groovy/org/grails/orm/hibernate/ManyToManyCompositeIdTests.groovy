package org.grails.orm.hibernate

import static junit.framework.Assert.*
import org.junit.Test


class ManyToManyCompositeIdTests extends AbstractGrailsHibernateTests {

    @Test
    void testManyToManyDomain() {
        def ManyToManyCompositeIdAuthor = ga.getDomainClass(ManyToManyCompositeIdAuthor.name)
        def bookDomain = ga.getDomainClass(ManyToManyCompositeIdBook.name)

        def books = ManyToManyCompositeIdAuthor?.getPropertyByName("books")
        def authors = bookDomain?.getPropertyByName("authors")

        assert books?.isManyToMany()
        assert authors?.isManyToMany()
        assert !books?.isOneToMany()
        assert !authors?.isOneToMany()
    }

    @Test
    void testManyToManyMapping() {

        def a = ManyToManyCompositeIdAuthor.newInstance(name:"Stephen King", family:10, child:1)

        a.addToBooks(ManyToManyCompositeIdBook.newInstance(title:"The Shining", isbn:5001, edition:2))
         .addToBooks(ManyToManyCompositeIdBook.newInstance(title:"The Stand", isbn:3402, edition:4))
         .save(true)
        assertEquals 2, ManyToManyCompositeIdBook.list().size()

        def b = ManyToManyCompositeIdBook.get(ManyToManyCompositeIdBook.newInstance(isbn:5001, edition:2))
        assert b
        assertNotNull b.authors
        assertEquals 1, b.authors.size()

        a = ManyToManyCompositeIdAuthor.get(ManyToManyCompositeIdAuthor.newInstance(family:10, child:1))
        assert a
        assertNotNull a.books
        assertEquals 2, a.books.size()

        assertEquals b, a.books.find { (it.isbn == 5001) && (it.edition == 2) }
        session.flush()
        session.clear()

        a = ManyToManyCompositeIdAuthor.get(ManyToManyCompositeIdAuthor.newInstance(family:10, child:1))
        assert a
        assert a.books

        b = ManyToManyCompositeIdBook.get(ManyToManyCompositeIdBook.newInstance(isbn:5001, edition:2))
        assert b
        assert b.authors

        // now let's test the database state
        def c = session.connection()

        def ps = c.prepareStatement("select * from many_to_many_composite_id_author_books order by many_to_many_composite_id_book_isbn")
        def rs = ps.executeQuery()
        assert rs.next()

        assertEquals 10,rs.getInt("many_to_many_composite_id_author_family")
        assertEquals 1,rs.getInt("many_to_many_composite_id_author_child")
        assertEquals 3402,rs.getInt("many_to_many_composite_id_book_isbn")
        assertEquals 4,rs.getInt("many_to_many_composite_id_book_edition")

        assert rs.next()
        assertEquals 10,rs.getInt("many_to_many_composite_id_author_family")
        assertEquals 1,rs.getInt("many_to_many_composite_id_author_child")
        assertEquals 5001,rs.getInt("many_to_many_composite_id_book_isbn")
        assertEquals 2,rs.getInt("many_to_many_composite_id_book_edition")
    }

    @Override
    protected getDomainClasses() {
        [ManyToManyCompositeIdBook, ManyToManyCompositeIdAuthor]
    }
}

class ManyToManyCompositeIdBook implements Serializable {
    Long id
    Long version

    Long isbn
    Long edition
    String title
    static belongsTo = ManyToManyCompositeIdAuthor
    Set authors
    static hasMany = [authors:ManyToManyCompositeIdAuthor]

    static mapping = {
        id composite:['isbn', 'edition'], generator:'assigned'
    }
}

class ManyToManyCompositeIdAuthor implements Serializable {
    Long id
    Long version

    Long family
    Long child
    String name
    Set books
    static hasMany = [books:ManyToManyCompositeIdBook]

    static mapping = {
        id composite:['family', 'child'], generator:'assigned'
    }
}
