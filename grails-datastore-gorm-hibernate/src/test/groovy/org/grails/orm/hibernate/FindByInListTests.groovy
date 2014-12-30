package org.grails.orm.hibernate

import grails.persistence.Entity;

import org.junit.Test

import static junit.framework.Assert.*


/**
 * @author Graeme Rocher
 * @since 1.0
 */
class FindByInListTests extends AbstractGrailsHibernateTests {

    @Test
    void testFindInList() {
        createBooks()

        def results = FindByInListBook.findAllByTitleInList(['The Shining', 'Rose Madder'])

        assertNotNull results
        assertEquals 2, results.size()

        assertTrue "Should have returned 'The Shining' from inList query", results.any { it.title = "The Shining" }
        assertTrue "Should have returned 'Rose Madder' from inList query", results.any { it.title = "Rose Madder" }
    }

    @Test
    void testFindInListWithGStrings() {
        createBooks()

        def results = FindByInListBook.findAllByTitleInList(["${'The Shining'}", "${'Rose Madder'}"].asImmutable())

        assertNotNull results
        assertEquals 2, results.size()

        assertTrue "Should have returned 'The Shining' from inList query", results.any { it.title = "The Shining" }
        assertTrue "Should have returned 'Rose Madder' from inList query", results.any { it.title = "Rose Madder" }
    }
    
    @Test
    void testFindInListEmpty() {
        createBooks()

        def results = FindByInListBook.findAllByTitleInList([])
        assertEquals 0, results.size()
    }

    @Test
    void testFindInListEmptyUsingOr() {
        createBooks()

        def results = FindByInListBook.findAllByTitleInListOrTitle([], 'The Stand')
        assertEquals 1, results.size()

        assertEquals "Should have returned 'The Stand' from inList query", 'The Stand', results[0].title
    }

    @Test
    void testMultipleFindInListEmptyUsingOr() {
        createBooks()

        def results = FindByInListBook.findAllByTitleInListOrAuthorInList([], [])
        assertEquals 0, results.size()
    }

    @Test
    void testNullArgumentsToInListQueries() {
        createBooks()

        def results = FindByInListBook.findAllByTitleInListOrAuthorInList(null, null)
        assertEquals 0, results.size()

        results = FindByInListBook.findAllByTitleInListOrTitle(null, 'The Stand')
        assertEquals 1, results.size()
        assertEquals "Should have returned 'The Stand' from inList query", 'The Stand', results[0].title
    }

    private void createBooks() {
        assertNotNull FindByInListBook.newInstance(title:"The Stand", author: "Stephen King").save(flush:true)
        assertNotNull FindByInListBook.newInstance(title:"The Shining", author: "Stephen King").save(flush:true)
        assertNotNull FindByInListBook.newInstance(title:"Rose Madder", author: "Stephen King").save(flush:true)
        assertNotNull FindByInListBook.newInstance(title:"Daemon", author: "Daniel Suarez").save(flush:true)
        session.clear()
    }

    @Override
    protected getDomainClasses() {
        [FindByInListBook]
    }
}


@Entity
class FindByInListBook {
    Long id
    Long version

    String title
    String author
}