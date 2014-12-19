package org.grails.orm.hibernate

import grails.persistence.Entity
import org.grails.orm.hibernate.cfg.GrailsHibernateUtil

import static junit.framework.Assert.*
import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class OneToOneLazinessTests extends AbstractGrailsHibernateTests {

    @Test
    void testManyToOneLaziness() {
        def author = OneToOneLazinessTestsAuthor.newInstance(name:"Stephen King", book:OneToOneLazinessTestsBook.newInstance(title:"The Stand"))
        assertNotNull author.save()

        session.clear()

        author = OneToOneLazinessTestsAuthor.get(1)
        assertFalse "one-to-one association should have been lazy loaded", GrailsHibernateUtil.isInitialized(author, "book")
        assertEquals "The Stand", author.book.title
        assertTrue "lazy one-to-one association should have been initialized",GrailsHibernateUtil.isInitialized(author, "book")
    }

    @Test
    void testDynamicFinderWithLazyProxy() {
        def author = OneToOneLazinessTestsAuthor.newInstance(name:"Stephen King", book:OneToOneLazinessTestsBook.newInstance(title:"The Stand"))
        assertNotNull author.save()

        session.clear()

        author = OneToOneLazinessTestsAuthor.get(1)
        def book = GrailsHibernateUtil.getAssociationProxy(author, "book")
        author.discard()
        assertFalse "one-to-one association should have been lazy loaded", GrailsHibernateUtil.isInitialized(author, "book")
        assertNotNull "Finders with dynamic proxies aren't working!", OneToOneLazinessTestsAuthor.findByBook(book)
    }

    @Override
    protected getDomainClasses() {
        [OneToOneLazinessTestsBook, OneToOneLazinessTestsAuthor]
    }
}

@Entity
class OneToOneLazinessTestsBook {
    Long id
    Long version

    String title
    OneToOneLazinessTestsAuthor author
    static belongsTo = [author:OneToOneLazinessTestsAuthor]
}

@Entity
class OneToOneLazinessTestsAuthor {
    Long id
    Long version

    String name
    OneToOneLazinessTestsBook book
}
