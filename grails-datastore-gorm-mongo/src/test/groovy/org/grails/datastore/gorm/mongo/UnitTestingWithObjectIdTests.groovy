package org.grails.datastore.gorm.mongo

import grails.datastore.test.DatastoreUnitTestMixin
import grails.persistence.Entity
import org.bson.types.ObjectId

/**
 * Tests that ObjectId can be used in unit tests
 */
@Mixin(DatastoreUnitTestMixin)
class UnitTestingWithObjectIdTests extends GroovyTestCase{

    void testThatObjectIdCanBeUsedToPersist() {
        mockDomain(Thing)
        def t = new Thing(name:"Bob")

        assert t.save(flush:true) != null
        session.clear()

        t = Thing.get(t.id)

        assert t != null
        assert Thing.count() == 1
    }
}
@Entity
class Thing {
    ObjectId id
    Long version
    String name
}
