package org.grails.orm.hibernate

import grails.persistence.Entity
import org.hibernate.FlushMode
import org.springframework.dao.DataAccessException

import org.junit.Test

import static junit.framework.Assert.*

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class DontFlushAfterDataAccessExceptionTests extends AbstractGrailsHibernateTests {

    @Test
    void testDontFlushAfterDataAccessException() {
        session.setFlushMode(FlushMode.AUTO)
        assertNotNull DontFlushAfterDataAccessExceptionAuthor.newInstance(name:"bob")
                            .addToBooks(name:"my story")
                            .save(flush:true)

        assertEquals FlushMode.AUTO, session.getFlushMode()

        session.clear()

        def author = DontFlushAfterDataAccessExceptionAuthor.get(1)

        shouldFail(DataAccessException) {
            author.delete(flush:true)
        }

        assertEquals FlushMode.MANUAL, session.getFlushMode()
    }

    @Override
    protected getDomainClasses() {
        [DontFlushAfterDataAccessExceptionAuthor, DontFlushAfterDataAccessExceptionBook]
    }
}

@Entity
class DontFlushAfterDataAccessExceptionAuthor {

    Long id
    Long version

    Set books
    static hasMany = [books: DontFlushAfterDataAccessExceptionBook]

    String name

    static mapping = {
        columns {
            books cascade: 'save-update'
        }
    }
}

@Entity
class DontFlushAfterDataAccessExceptionBook {

    Long id
    Long version

    static belongsTo = [author: DontFlushAfterDataAccessExceptionAuthor]

    DontFlushAfterDataAccessExceptionAuthor author

    String name
}
