package org.grails.orm.hibernate

import grails.orm.PagedResultList

import org.junit.Test

import static junit.framework.Assert.*

/**
 * @author Siegfried Puchbauer
 */
class CreateCriteriaTests extends AbstractGrailsHibernateTests {

    @Test
    void testCreateCriteriaMethod() {
        def books = []
        25.times {
            def book = CreateCriteriaMethodBook.newInstance()
            book.title = "Book $it"
            books << book
        }
        books*.save(true)

        def results = CreateCriteriaMethodBook.createCriteria().list(max: 10, offset: 0) {
            like("title","Book%")
        }

        assertEquals 10, results?.size()
        assertEquals 25, results?.totalCount
    }

    @Test
    void testPaginatedQueryReturnsPagedResultList() {
        def stats = sessionFactory.statistics
        stats.statisticsEnabled = true
        def books = []
        8.times {
            def book = CreateCriteriaMethodBook.newInstance()
            book.title = "Good Book $it"
            books << book
            book = CreateCriteriaMethodBook.newInstance()
            book.title = "Bad Book $it"
            books << book
        }
        books*.save(true)
        stats.clear()
        def results = CreateCriteriaMethodBook.createCriteria().list(max: 3, offset: 0) {
            like("title","Good Book%")
        }
        assertTrue 'results should have been a PagedResultList', results instanceof PagedResultList
        assertEquals 1, stats.queryExecutionCount

        assertEquals 3, results.size()
        assertEquals 8, results.totalCount
        assertEquals 2, stats.queryExecutionCount

        // refer to totalCount again and make sure another query was not sent to the database
        assertEquals 8, results.totalCount
        assertEquals 2, stats.queryExecutionCount
    }

    @Override
    protected getDomainClasses() {
        [CreateCriteriaMethodBook]
    }
}
class CreateCriteriaMethodBook {
    Long id
    Long version
    String title

    boolean equals(obj) { title == obj?.title }
    int hashCode() { title ? title.hashCode() : super.hashCode() }
    String toString() { title }

    static constraints = {
        title(nullable:false)
    }
}

