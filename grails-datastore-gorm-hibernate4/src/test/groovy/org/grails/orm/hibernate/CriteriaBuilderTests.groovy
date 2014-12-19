package org.grails.orm.hibernate

import grails.persistence.Entity
import org.hibernate.Hibernate

import org.junit.Test

import static junit.framework.Assert.*

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Oct 27, 2008
 */
class CriteriaBuilderTests extends AbstractGrailsHibernateTests {

    @Test
    void testIdEq() {
        assertNotNull CriteriaBuilderAuthor.newInstance(name:"Stephen King")
                                 .addToBooks(title:"The Shining")
                                 .addToBooks(title:"The Stand")
                                 .addToBooks(title:"Rose Madder")
                                 .save(flush:true)

        assertNotNull CriteriaBuilderAuthor.newInstance(name:"James Patterson")
                                 .addToBooks(title:"Along Came a Spider")
                                 .addToBooks(title:"A Time to Kill")
                                 .addToBooks(title:"Killing Me Softly")
                                 .addToBooks(title:"The Quickie")
                                 .save(flush:true)

        session.clear()
        def book = CriteriaBuilderBook.findByTitle("The Quickie")

        assertNotNull "should have found book", book

        def authors = CriteriaBuilderAuthor.withCriteria {
            books {
                idEq book.id
            }
        }

        assertNotNull "should have returned a list of authors", authors
        assertEquals 1, authors.size()
        assertEquals "James Patterson", authors[0].name
    }

    @Test
    void testSizeCriterion() {
        assertNotNull CriteriaBuilderAuthor.newInstance(name:"Stephen King")
                                 .addToBooks(title:"The Shining")
                                 .addToBooks(title:"The Stand")
                                 .addToBooks(title:"Rose Madder")
                                 .save(flush:true)

        assertNotNull CriteriaBuilderAuthor.newInstance(name:"James Patterson")
                                 .addToBooks(title:"Along Came a Spider")
                                 .addToBooks(title:"A Time to Kill")
                                 .addToBooks(title:"Killing Me Softly")
                                 .addToBooks(title:"The Quickie")
                                 .save(flush:true)

        def results = CriteriaBuilderAuthor.withCriteria {
            sizeGt('books', 3)
        }
        assertEquals 1, results.size()

        results = CriteriaBuilderAuthor.withCriteria {
            sizeGe('books', 3)
        }
        assertEquals 2, results.size()

        results = CriteriaBuilderAuthor.withCriteria {
            sizeNe('books', 1)
        }
        assertEquals 2, results.size()

        results = CriteriaBuilderAuthor.withCriteria {
            sizeNe('books', 3)
        }
        assertEquals 1, results.size()

        results = CriteriaBuilderAuthor.withCriteria {
            sizeLt('books', 4)
        }
        assertEquals 1, results.size()

        results = CriteriaBuilderAuthor.withCriteria {
            sizeLe('books', 4)
        }
        assertEquals 2, results.size()
    }

    @Test
    void testCacheMethod() {

        def author = CriteriaBuilderAuthor.newInstance(name:"Stephen King")
                                .addToBooks(title:"The Shining")
                                .addToBooks(title:"The Stand")
                                .save(flush:true)

        assertNotNull author

        session.clear()




        // NOTE: note sure how to actually test the cache, I'm testing
        // that invoking the cache method works but need a better test
        // that ensure entries are pulled from the cache

        def authors = CriteriaBuilderAuthor.withCriteria {
            eq('name', 'Stephen King')
            cache false

            def criteriaInstance = getInstance()
            assertFalse criteriaInstance.cacheable
        }

        assertEquals 1, authors.size()
    }

    @Test
    void testLockMethod() {

        // NOTE: HSQLDB doesn't support the SQL SELECT..FOR UPDATE syntax so this test
        // is basically just testing that the lock method can be called without error


        def author = CriteriaBuilderAuthor.newInstance(name:"Stephen King")
                                .addToBooks(title:"The Shining")
                                .addToBooks(title:"The Stand")
                                .save(flush:true)
        assertNotNull author

        session.clear()

        def authors = CriteriaBuilderAuthor.withCriteria {
            eq('name', 'Stephen King')
            lock true
        }

        assert authors
    
        // test lock association

        try {
            authors = CriteriaBuilderAuthor.withCriteria {
                eq('name', 'Stephen King')
                books {
                    lock true
                }
            }
    
            assert authors
        } catch (Exception e) {
            // workaround for h2 issue https://code.google.com/p/h2database/issues/detail?id=541
            if(!e.cause?.message?.contains("Feature not supported")) {
                throw e
            }
        }
    }

    @Test
    void testJoinMethod() {

        def author = CriteriaBuilderAuthor.newInstance(name:"Stephen King")
                                .addToBooks(title:"The Shining")
                                .addToBooks(title:"The Stand")
                                .save(flush:true)

        assertNotNull author

        session.clear()

        def authors = CriteriaBuilderAuthor.withCriteria {
            eq('name', 'Stephen King')
        }
        assert authors
        author = authors[0]

        assertFalse "books association is lazy by default and shouldn't be initialized",Hibernate.isInitialized(author.books)

        session.clear()

        authors = CriteriaBuilderAuthor.withCriteria {
            eq('name', 'Stephen King')
            join "books"
        }
        author = authors[0]

        assertTrue "books association loaded with join query and should be initialized",Hibernate.isInitialized(author.books)
    }

    @Override
    protected getDomainClasses() {
        [CriteriaBuilderAuthor, CriteriaBuilderBook]
    }
}

@Entity
class CriteriaBuilderBook {
    Long id
    Long version

    String title
    CriteriaBuilderAuthor author
    static belongsTo = [author:CriteriaBuilderAuthor]

}

@Entity
class CriteriaBuilderAuthor {
    Long id
    Long version

    String name
    Set books
    static hasMany = [books:CriteriaBuilderBook]


}
