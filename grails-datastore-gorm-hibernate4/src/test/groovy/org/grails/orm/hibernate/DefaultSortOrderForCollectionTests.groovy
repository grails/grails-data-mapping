package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test

import static junit.framework.Assert.*

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Jun 23, 2009
 */
class DefaultSortOrderForCollectionTests extends AbstractGrailsHibernateTests {

    @Test
    void testDefaultSortOrderWithCollection() {
        def a = DefaultSortOrderForCollectionAuthor.newInstance()
                      .addToBooks(bookTitle:"It")
                      .addToBooks(bookTitle:"Stand by me")
                      .addToBooks(bookTitle:"Along came a spider")
                      .save(flush:true)

        session.clear()

        a = DefaultSortOrderForCollectionAuthor.get(1)
        def books = a.books.toList()

        assertEquals "Along came a spider", books[0].bookTitle
        assertEquals "It", books[1].bookTitle
        assertEquals "Stand by me", books[2].bookTitle
    }

    @Override
    protected getDomainClasses() {
        [DefaultSortOrderForCollectionAuthor, DefaultSortOrderForCollectionBook]
    }
}


@Entity
class DefaultSortOrderForCollectionBook {
    Long id
    Long version

    String bookTitle
    DefaultSortOrderForCollectionAuthor author
    static belongsTo = [author:DefaultSortOrderForCollectionAuthor]
}

@Entity
class DefaultSortOrderForCollectionAuthor {
    Long id
    Long version

    Set books
    static hasMany = [books:DefaultSortOrderForCollectionBook]
    static mapping = { books sort:'bookTitle' }
}
