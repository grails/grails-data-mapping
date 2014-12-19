package org.grails.orm.hibernate

import grails.persistence.Entity
import org.grails.orm.hibernate.cfg.GrailsHibernateUtil

import static junit.framework.Assert.*
import org.junit.Test

 /**
* @author Graeme Rocher
* @since 1.0
*
* Created: Jan 17, 2008
*/
class ManyToOneLazinessTests extends AbstractGrailsHibernateTests {

    @Test
    void testManyToOneLaziness() {
        def author = ManyToOneLazinessTestsAuthor.newInstance(name:"Stephen King")
        assertNotNull author.save()

        author.addToBooks(title:"The Stand")
              .addToBooks(title:"The Shining")
              .save(flush:true)

        session.clear()

        def book = ManyToOneLazinessTestsBook.get(1)
        assertFalse "many-to-one association should have been lazy loaded", GrailsHibernateUtil.isInitialized(book, "author")
        assertEquals "Stephen King", book.author.name
        assertTrue "lazy many-to-one association should have been initialized",GrailsHibernateUtil.isInitialized(book, "author")
    }

    @Override
    protected getDomainClasses() {
        [ManyToOneLazinessTestsBook,ManyToOneLazinessTestsAuthor]
    }
}


@Entity
class ManyToOneLazinessTestsBook {
    Long id
    Long version

    String title
    ManyToOneLazinessTestsAuthor author
    static belongsTo =  ManyToOneLazinessTestsAuthor

    static mapping = {
        author lazy:true
    }
}

@Entity
class ManyToOneLazinessTestsAuthor {
    Long id
    Long version

    String name
    Set books
    static hasMany = [books:ManyToOneLazinessTestsBook]
}
