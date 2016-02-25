package org.grails.orm.hibernate

import grails.persistence.Entity

import static junit.framework.Assert.*
import org.junit.Test

class ListDomainTests extends AbstractGrailsHibernateTests{

    @Test
    void testListDomain() {
        def authorClass = ga.getDomainClass(ListDomainAuthor.name)
        def bookClass = ga.getDomainClass(ListDomainBook.name)

        def authorsProp = bookClass.getPropertyByName("authors")
        assertTrue authorsProp.oneToMany
        assertTrue authorsProp.bidirectional
        assertTrue authorsProp.association
        assertEquals "book", authorsProp.referencedPropertyName

        def otherSide = authorsProp.otherSide
        assertNotNull otherSide
        assertEquals "book", otherSide.name
    }

    @Override
    protected getDomainClasses() {
        [ListDomainBook, ListDomainAuthor]
    }
}

@Entity
class ListDomainBook {
    Long id
    Long version
    List authors
    static hasMany = [authors:ListDomainAuthor]
}

@Entity
class ListDomainAuthor {
    Long id
    Long version
    ListDomainBook book
}
