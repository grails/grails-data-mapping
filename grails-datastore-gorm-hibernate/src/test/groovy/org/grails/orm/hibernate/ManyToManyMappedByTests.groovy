package org.grails.orm.hibernate

import static junit.framework.Assert.*
import grails.persistence.Entity;

import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Dec 4, 2007
 */
class ManyToManyMappedByTests extends AbstractGrailsHibernateTests {



    @Test
    void testDomain() {
        def bookClass = ga.getDomainClass(ManyToManyMappedByBook.name)
        def authorClass = ga.getDomainClass(ManyToManyMappedByAuthor.name)

        assertTrue bookClass.getPropertyByName("d").manyToMany
        assertTrue bookClass.getPropertyByName("d").bidirectional
        assertEquals(authorClass.getPropertyByName("books"), bookClass.getPropertyByName("d").otherSide)
        assertTrue authorClass.getPropertyByName("books").manyToMany
        assertTrue authorClass.getPropertyByName("books").bidirectional
    }

    @Test
    void testMapping() {
        assertNotNull ManyToManyMappedByAuthor.newInstance(email:"foo@bar.com").save(flush:true)

        def a = ManyToManyMappedByAuthor.get(1)
        def book = ManyToManyMappedByBook.newInstance(creator:a, title:"The Stand")
        a.addToBooks(book)
        a.save(flush:true)

        session.clear()

        a = ManyToManyMappedByAuthor.get(1)
        assertEquals 1, a.books.size()

        book = ManyToManyMappedByBook.get(1)
        assertNotNull book.creator
        assertEquals 1, book.d.size()
    }

    @Override
    protected getDomainClasses() {
        [ManyToManyMappedByAuthor, ManyToManyMappedByBook]
    }
}

@Entity
class ManyToManyMappedByBook implements Serializable{
    Long id
    Long version
    String title

    ManyToManyMappedByAuthor creator

    Set d
    static hasMany = [d: ManyToManyMappedByAuthor]
    static belongsTo = [ManyToManyMappedByAuthor]
}

@Entity
class ManyToManyMappedByAuthor implements Serializable {
    Long id
    Long version
    String email
    Set books
    static hasMany = [books: ManyToManyMappedByBook]
    static mappedBy = [books:'d']
}
