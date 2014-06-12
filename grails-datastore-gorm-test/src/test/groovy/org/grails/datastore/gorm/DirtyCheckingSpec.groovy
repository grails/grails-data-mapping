package org.grails.datastore.gorm

import grails.gorm.tests.GormDatastoreSpec

import grails.gorm.tests.Person
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable

/**
 * @author Graeme Rocher
 */
class DirtyCheckingSpec extends GormDatastoreSpec {

    void "Test that dirty checking methods work when changing entities"() {
        when:"A new instance is created"
            def p = new Person(firstName: "Homer", lastName: "Simpson")

        then:"The instance is dirty by default"
            p instanceof DirtyCheckable
            p.isDirty()
            p.isDirty("firstName")

        when:"The instance is saved"
            p.save(flush:true)

        then:"The instance is no longer dirty"
            !p.isDirty()
            !p.isDirty("firstName")

        when:"The instance is changed"
            p.firstName = "Bart"

        then:"The instance is now dirty"
            p.isDirty()
            p.isDirty("firstName")
            p.dirtyPropertyNames == ['firstName']
            p.getPersistentValue('firstName') == "Homer"

        when:"The instance is loaded from the db"
            p.save(flush:true)
            session.clear()
            p = Person.get(p.id)

        then:"The instance is not dirty"
            !p.isDirty()
            !p.isDirty('firstName')

        when:"The instance is changed"
            p.firstName = "Lisa"

        then:"The instance is dirty"
            p.isDirty()
            p.isDirty("firstName")


    }
}
