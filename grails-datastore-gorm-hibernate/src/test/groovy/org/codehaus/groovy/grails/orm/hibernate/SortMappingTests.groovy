package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity
import static junit.framework.Assert.*
import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Oct 27, 2008
 */
class SortMappingTests extends AbstractGrailsHibernateTests {


    @Test
    void testDefaultAssociationSortOrderWithDirection() {
        def author = SortMappingAuthor2.newInstance(name:"John")
                                .addToBooks(title:"E")
                                .addToBooks(title:"C")
                                .addToBooks(title:"Z")
                                .addToBooks(title:"A")
                                .addToBooks(title:"K")
                                .save(flush:true)
        assertNotNull author

        session.clear()

        author = SortMappingAuthor2.get(1)
        assertNotNull author
        def books = author.books.toList()
        assertEquals "Z", books[0].title
        assertEquals "K", books[1].title
        assertEquals "E", books[2].title
        assertEquals "C", books[3].title
        assertEquals "A", books[4].title
    }

    @Test
    void testDefaultSortOrderWithFinder() {
        assertNotNull SortMappingAuthor.newInstance(name:"Stephen King").save(flush:true)
        assertNotNull SortMappingAuthor.newInstance(name:"Lee Child").save(flush:true)
        assertNotNull SortMappingAuthor.newInstance(name:"James Patterson").save(flush:true)
        assertNotNull SortMappingAuthor.newInstance(name:"Dean Koontz").save(flush:true)

        session.clear()

        def authors = SortMappingAuthor.findAllByNameLike("%e%")

        assertEquals "Dean Koontz", authors[0].name
        assertEquals "James Patterson", authors[1].name
        assertEquals "Lee Child", authors[2].name
        assertEquals "Stephen King", authors[3].name
    }

    @Test
    void testDefaultSortOrder() {
        assertNotNull SortMappingAuthor.newInstance(name:"Stephen King").save(flush:true)
        assertNotNull SortMappingAuthor.newInstance(name:"Lee Child").save(flush:true)
        assertNotNull SortMappingAuthor.newInstance(name:"James Patterson").save(flush:true)
        assertNotNull SortMappingAuthor.newInstance(name:"Dean Koontz").save(flush:true)

        session.clear()

        def authors = SortMappingAuthor.list()
        assertEquals "Dean Koontz", authors[0].name
        assertEquals "James Patterson", authors[1].name
        assertEquals "Lee Child", authors[2].name
        assertEquals "Stephen King", authors[3].name
    }

    @Test
    void testDefaultSortOrderMapSyntax() {
        def author = SortMappingAuthor.newInstance(name:"John")
                                .addToBooks(title:"E")
                                .addToBooks(title:"C")
                                .addToBooks(title:"Z")
                                .addToBooks(title:"A")
                                .addToBooks(title:"K")
                                .save(flush:true)

        assertNotNull author

        session.clear()

        def books = SortMappingBook.list()
        assertEquals "Z", books[0].title
        assertEquals "K", books[1].title
        assertEquals "E", books[2].title
        assertEquals "C", books[3].title
        assertEquals "A", books[4].title
    }

    @Test
    void testSortMapping() {
        def author = SortMappingAuthor.newInstance(name:"John")
                                .addToBooks(title:"E")
                                .addToBooks(title:"C")
                                .addToBooks(title:"Z")
                                .addToBooks(title:"A")
                                .addToBooks(title:"K")
                                .save(flush:true)

        assertNotNull author

        session.clear()

        author = SortMappingAuthor.get(1)
        assertNotNull author
        def books = author.books.toList()
        assertEquals "A", books[0].title
        assertEquals "C", books[1].title
        assertEquals "E", books[2].title
        assertEquals "K", books[3].title
        assertEquals "Z", books[4].title
    }

    @Override
    protected getDomainClasses() {
        [SortMappingAuthor2, SortMappingAuthor, SortMappingBook, SortMappingBook2]
    }
}

@Entity
class SortMappingBook {
    Long id
    Long version

    String title
    SortMappingAuthor author
    static belongsTo = [author:SortMappingAuthor]

    static mapping = {
        sort title:'desc'
    }
}

@Entity
class SortMappingAuthor {

    Long id
    Long version

    String name
    Set unibooks

    Set books
    static hasMany = [books:SortMappingBook]

    static mapping = {
        sort 'name'
        books sort:'title'
    }
}

@Entity
class SortMappingBook2 {
    Long id
    Long version

    String title
    SortMappingAuthor2 author
    static belongsTo = [author:SortMappingAuthor2]

    static mapping = {
        sort title:'desc'
    }
}

@Entity
class SortMappingAuthor2 {

    Long id
    Long version

    String name
    Set unibooks

    Set books
    static hasMany = [books:SortMappingBook2]

    static mapping = {
        sort 'name'
        books sort:'title', order:"desc"
    }
}
