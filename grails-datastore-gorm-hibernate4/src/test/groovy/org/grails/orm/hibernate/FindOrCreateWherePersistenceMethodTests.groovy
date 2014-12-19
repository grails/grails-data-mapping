package org.grails.orm.hibernate

import org.junit.Test
import static junit.framework.Assert.*

class FindOrCreateWherePersistenceMethodTests extends AbstractGrailsHibernateTests {

    protected getDomainClasses() {
        [Person, Pet, Face, Nose]
    }

    @Test
    void testFindOrCreateWhereForNonExistingRecord() {
        def person = Person.findOrCreateWhere(firstName: 'Robert', lastName: 'Fripp')

        assertNotNull 'findOrCreateWhere should have returned a Person', person
        assertEquals 'Robert', person.firstName
        assertEquals 'Fripp', person.lastName
    }

    @Test
    void testFindOrCreateWhereForExistingRecord() {
        def person = Person.newInstance()
        person.firstName = 'Adrian'
        person.lastName = 'Belew'
        assertNotNull 'save failed', person.save()

        def personId = person.id
        assertNotNull 'id should not have been ull', personId

        person = Person.findOrCreateWhere(firstName: 'Adrian', lastName: 'Belew')
        assertNotNull 'findOrCreateWhere should not have returned null', person
        assertEquals 'Adrian', person.firstName
        assertEquals 'Belew', person.lastName
        assertEquals personId, person.id
    }
}

