package org.grails.orm.hibernate

import grails.persistence.Entity
import org.hibernate.*
import org.junit.Test
import static junit.framework.Assert.*

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Oct 27, 2008
 */
class FetchMappingTests extends AbstractGrailsHibernateTests {

    @Test
    void testFetchMapping() {
        def author = FetchMappingAuthor.newInstance(name: "Stephen King")
                .addToBooks(title: "The Shining")
                .addToBooks(title: "The Stand")
                .save(flush: true)

        def publisher = FetchMappingPublisher.newInstance(name: "Apress")
                .addToBooks(title: "DGG")
                .addToBooks(title: "BGG")
                .save(flush: true)

        assertNotNull author
        assertNotNull publisher

        session.clear()

        author = FetchMappingAuthor.get(1)
        assertFalse "books association is lazy by default and shouldn't be initialized", Hibernate.isInitialized(author.books)

        publisher = FetchMappingPublisher.get(1)
        assertTrue "books association mapped with join query and should be initialized", Hibernate.isInitialized(publisher.books)
    }

    @Override
    protected getDomainClasses() {
        [FetchMappingBook, FetchMappingAuthor, FetchMappingPublisher]
    }
}

@Entity
class FetchMappingBook {
    Long id
    Long version

    String title
}

@Entity
class FetchMappingAuthor {
    Long id
    Long version

    String name
    Set books
    static hasMany = [books: FetchMappingBook]
}

@Entity
class FetchMappingPublisher {
    Long id
    Long version

    String name
    Set books
    static hasMany = [books: FetchMappingBook]

    static mapping = {
        books fetch: 'join'
    }
}
