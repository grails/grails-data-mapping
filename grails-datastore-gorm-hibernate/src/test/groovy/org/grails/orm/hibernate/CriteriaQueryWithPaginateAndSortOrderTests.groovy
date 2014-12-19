package org.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test

import static junit.framework.Assert.*

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class CriteriaQueryWithPaginateAndSortOrderTests extends AbstractGrailsHibernateTests {

    @Test
    void testCriteriaWithSortOrderAndPagination() {
        assertNotNull "should have saved", CriteriaQueryWithPaginateAndSortOrderExample.newInstance(toSort:"string 1").save(flush:true)
        assertNotNull "should have saved", CriteriaQueryWithPaginateAndSortOrderExample.newInstance(toSort:"string 2").save(flush:true)

        session.clear()

        CriteriaQueryWithPaginateAndSortOrderExample.createCriteria().list(max: 32, offset: 0) {
            and {
                like('toSort', '%string%')
            }
            order("toSort", "asc")
        }
    }

    @Override
    protected getDomainClasses() {
        [CriteriaQueryWithPaginateAndSortOrderExample]
    }
}

@Entity
class CriteriaQueryWithPaginateAndSortOrderExample {
    Long id
    Long version

    String toSort
}
