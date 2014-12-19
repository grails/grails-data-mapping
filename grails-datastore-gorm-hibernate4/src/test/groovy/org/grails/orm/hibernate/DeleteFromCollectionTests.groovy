package org.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test

import static junit.framework.Assert.*

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Nov 22, 2007
 */
class DeleteFromCollectionTests extends AbstractGrailsHibernateTests {

    @Test
    void testDeleteFromCollection() {

        DeleteAuthor.newInstance(name:"Stephen King")
                   .addToBooks(title:"The Stand")
                   .addToBooks(title:"The Shining")
                   .save(flush:true)

        session.clear()

        def author = DeleteAuthor.get(1)
        assertNotNull author
        assertEquals 2, author.books.size()

        def book1 = author.books.find { it.title.endsWith("Stand") }
        author.removeFromBooks(book1)
        book1.delete(flush:true)

        session.clear()

        author = DeleteAuthor.get(1)
        assertNotNull author
        assertEquals 1, author.books.size()
    }

    @Override
    protected getDomainClasses() {
        [DeleteBook, DeleteAuthor]
    }
}


@Entity
class DeleteBook {
    Long id
    Long version
    String title
    DeleteAuthor author
    static belongsTo = DeleteAuthor
}

@Entity
class DeleteAuthor {
    Long id
    Long version
    String name
    Set books
    static hasMany = [books:DeleteBook]
}
