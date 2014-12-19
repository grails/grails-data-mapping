package org.grails.orm.hibernate

import grails.persistence.Entity
import org.springframework.dao.DuplicateKeyException
import org.springframework.orm.hibernate4.HibernateSystemException

import static junit.framework.Assert.*
import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class NaturalIdentifierTests extends AbstractGrailsHibernateTests {

    @Test
    void testNaturalIdentifier() {
        def a = NaturalAuthor.newInstance(name:"Stephen King").save(flush:true)
        def b = NaturalBook.newInstance(author:a, title:"The Stand").save(flush:true)

        assertNotNull b

        b.title = "Changed"

        // should fail with an attempt to alter an immutable natural identifier
        shouldFail(HibernateSystemException) {
            b.save(flush:true)
        }

        // should fail with a unique constraint violation exception
        shouldFail(DuplicateKeyException) {
            NaturalBook.newInstance(author:a, title:"The Stand").save(flush:true)
        }
    }

    @Test
    void testMutableNaturalIdentifier() {
        def a = NaturalAuthor.newInstance(name:"Stephen King").save(flush:true)

        def b = NaturalBook2.newInstance(author:a, title:"The Stand").save(flush:true)

        assertNotNull b

        b.title = "Changed"
        // mutable identifier so no problem
        b.save(flush:true)

        // should fail with a unique constraint violation exception
        shouldFail(DuplicateKeyException) {
            NaturalBook2.newInstance(author:a, title:"Changed").save(flush:true)
        }
    }

    @Override
    protected getDomainClasses() {
        [NaturalAuthor, NaturalBook, NaturalBook2]
    }
}


@Entity
class NaturalAuthor {
    Long id
    Long version

    String name
}

@Entity
class NaturalBook {
    Long id
    Long version

    String title
    NaturalAuthor author

    static mapping = {
        id natural:['title', 'author']
    }
}

@Entity
class NaturalBook2 {
    Long id
    Long version

    String title
    NaturalAuthor author

    static mapping = {
        id natural:[properties:['title', 'author'], mutable:true]
    }
}
