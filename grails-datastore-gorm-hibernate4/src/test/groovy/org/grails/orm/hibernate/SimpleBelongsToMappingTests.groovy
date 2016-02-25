package org.grails.orm.hibernate

import grails.persistence.Entity

import static junit.framework.Assert.*
import org.junit.Test


class SimpleBelongsToMappingTests extends AbstractGrailsHibernateTests {

    @Test
    void testListMapping() {
        def authorClass = ga.getDomainClass(SimpleBelongsToMappingAuthor.name)
        def bookClass = ga.getDomainClass(SimpleBelongsToMappingBook.name)

        assertEquals "author", authorClass.getPropertyByName("books").otherSide.name
    }

    @Override
    protected getDomainClasses() {
        [SimpleBelongsToMappingAuthor, SimpleBelongsToMappingBook]
    }
}

@Entity
class SimpleBelongsToMappingBook {
    Long id
    Long version
    SimpleBelongsToMappingAuthor author
    static belongsTo = [author:SimpleBelongsToMappingAuthor]
}

@Entity
class SimpleBelongsToMappingAuthor {
    Long id
    Long version
    String name
    Set books
    static hasMany = [books:SimpleBelongsToMappingBook]
}