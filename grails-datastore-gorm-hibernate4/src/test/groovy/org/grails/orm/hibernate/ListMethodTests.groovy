package org.grails.orm.hibernate

import grails.orm.PagedResultList

import static junit.framework.Assert.*
import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class ListMethodTests extends AbstractGrailsHibernateTests {


    @Test
    void testSortAndWithIgnoreCase() {
        ['A','C','b', 'a', 'c', 'B'].each { ListableBook.newInstance(title:it).save(flush:true) }

        assertEquals(['A','a','b','B',  'C', 'c'], ListableBook.list(sort:'title').title)
        assertEquals(['A','B','C', 'a', 'b', 'c'], ListableBook.list(sort:'title', ignoreCase:false).title)
    }

    @Test
    void testPaginatedQueryReturnsPagedResultList() {
        def stats = sessionFactory.statistics
        stats.statisticsEnabled = true

        ['A','C','b', 'a', 'c', 'B'].each { ListableBook.newInstance(title:it).save(flush:true) }

        stats.clear()
        def results = ListableBook.list(max: 2, offset: 0)
        assertTrue 'results should have been a PagedResultList', results instanceof PagedResultList
        assertEquals 1, stats.queryExecutionCount

        assertEquals 2, results.size()
        assertEquals 6, results.totalCount
        assertEquals 2, stats.queryExecutionCount

        // refer to totalCount again and make sure another query was not sent to the database
        assertEquals 6, results.totalCount
        assertEquals 2, stats.queryExecutionCount
    }

    @Override
    protected getDomainClasses() {
        [ListableBook]
    }
}

class ListableBook {
    Long id
    Long version
    String title
}
