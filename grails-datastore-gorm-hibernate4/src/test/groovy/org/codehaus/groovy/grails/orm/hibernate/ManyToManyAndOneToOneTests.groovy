package org.codehaus.groovy.grails.orm.hibernate

import grails.core.GrailsDomainClass

import static junit.framework.Assert.*
import org.junit.Test

/**
 * Tests a many-to-many and one-to-one relationship used together.
 *
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Oct 12, 2007
 */
class ManyToManyAndOneToOneTests extends AbstractGrailsHibernateTests {


    @Test
    void testDomain() {
        GrailsDomainClass bookClass = ga.getDomainClass(ManyToManyAndOneToOneBook.name)
        GrailsDomainClass authorClass = ga.getDomainClass(ManyToManyAndOneToOneAuthor.name)
        assertNotNull authorClass
        assertTrue authorClass.getPropertyByName("bookOther").isOneToOne()
        assertTrue authorClass.getPropertyByName("books").isManyToMany()
        assertEquals "authors",authorClass.getPropertyByName("books").otherSide.name
        assertFalse authorClass.getPropertyByName("bookOther").isBidirectional()

        assertTrue bookClass.getPropertyByName("authors").isManyToMany()
        assertEquals "books",bookClass.getPropertyByName("authors").otherSide.name
    }

    @Override
    protected getDomainClasses() {
        [ManyToManyAndOneToOneBook, ManyToManyAndOneToOneAuthor]
    }
}

class ManyToManyAndOneToOneBook {
    Long version
    Long id
    static belongsTo = ManyToManyAndOneToOneAuthor
    Set authors

    static hasMany = [authors:ManyToManyAndOneToOneAuthor]
    static mappedBy = [authors:"books"]
    String title
}

class ManyToManyAndOneToOneAuthor {
    Long version
    Long id

    Set books
    static hasMany = [books:ManyToManyAndOneToOneBook]
    static mappedBy = [books:"authors"]
    String name
    ManyToManyAndOneToOneBook bookOther
}