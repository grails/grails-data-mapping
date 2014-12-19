package org.grails.orm.hibernate

import static junit.framework.Assert.*
import org.junit.Test


class MapDomainTests extends AbstractGrailsHibernateTests{

    @Test
    void testMapDomain() {
        def authorClass = ga.getDomainClass(MapDomainAuthor.name)
        def bookClass = ga.getDomainClass(MapDomainBook.name)

        def simpleAuthors = bookClass.getPropertyByName("simpleAuthors")

        assertFalse simpleAuthors.association
        assertFalse simpleAuthors.oneToMany
        assertTrue simpleAuthors.persistent

        def authorsProp = bookClass.getPropertyByName("authors")
        assertTrue simpleAuthors.persistent
        assertTrue authorsProp.oneToMany
        assertTrue authorsProp.bidirectional
        assertTrue authorsProp.association
        assertEquals "book", authorsProp.referencedPropertyName
        assertEquals authorClass, authorsProp.referencedDomainClass
        assertEquals authorClass.clazz, authorsProp.referencedPropertyType
    }

    @Override
    protected getDomainClasses() {
        [MapDomainBook, MapDomainAuthor]
    }
}

class MapDomainBook {
    Long id
    Long version
    Map simpleAuthors
    Map authors
    static hasMany = [authors:MapDomainAuthor]
}

class MapDomainAuthor {
    Long id
    Long version
    MapDomainBook book
}
