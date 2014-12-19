package org.grails.orm.hibernate

import grails.persistence.Entity
import org.hibernate.LazyInitializationException

import static junit.framework.Assert.*
import org.junit.Test

class ManyToManyLazinessTests extends AbstractGrailsHibernateTests {

    @Test
    void testManyToManyLazyLoading() {
        def a = M2MLAuthor.newInstance()

        a.addToBooks(M2MLBook.newInstance())
        a.save()
        session.flush()

        session.evict(a)

        a = M2MLAuthor.get(1)
        session.evict(a)
        assertFalse session.contains(a)

        shouldFail(LazyInitializationException) {
            assertEquals 1, a.books.size()
        }
    }

    @Override
    protected getDomainClasses() {
        [M2MLAuthor, M2MLBook]
    }
}

@Entity
class M2MLBook {
    Long id
    Long version

    static belongsTo = M2MLAuthor
    Set authors
    static hasMany = [authors:M2MLAuthor]
}

@Entity
class M2MLAuthor {
    Long id
    Long version
    Set books
    static hasMany = [books:M2MLBook]
}

