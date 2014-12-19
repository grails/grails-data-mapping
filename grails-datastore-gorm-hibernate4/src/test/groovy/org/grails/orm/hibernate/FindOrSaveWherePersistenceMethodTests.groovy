package org.grails.orm.hibernate

import org.junit.Test
import static junit.framework.Assert.*

class FindOrSaveWherePersistenceMethodTests extends AbstractGrailsHibernateTests {

    protected getDomainClasses() {
        [Person, Pet, Face, Nose]
    }

    @Test
    void testFindOrSaveWhereForNonExistingRecord() {
        def person = Person.findOrSaveWhere(firstName: 'Robert', lastName: 'Fripp')

        assertNotNull 'findOrSaveWhere should have returned a Person', person
        assertEquals 'Robert', person.firstName
        assertEquals 'Fripp', person.lastName
        assertNotNull person.id
    }

    @Test
    void testFindOrSaveWhereForExistingRecord() {
        def person = Person.newInstance()
        person.firstName = 'Adrian'
        person.lastName = 'Belew'
        assertNotNull 'save failed', person.save()

        def personId = person.id
        assertNotNull 'id should not have been ull', personId

        person = Person.findOrSaveWhere(firstName: 'Adrian', lastName: 'Belew')
        assertNotNull 'findOrSaveWhere should not have returned null', person
        assertEquals 'Adrian', person.firstName
        assertEquals 'Belew', person.lastName
        assertEquals personId, person.id
    }
}

