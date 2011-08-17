package org.grails.datastore.gorm.mongo

import grails.datastore.test.DatastoreUnitTestMixin
import grails.persistence.Entity

import org.bson.types.ObjectId

@Mixin(DatastoreUnitTestMixin)
class UnitTestWithObjectIdTests extends GroovyTestCase {

    protected void setUp() {
        super.setUp()
        connect()
    }

    protected void tearDown() {
        disconnect()
        super.tearDown()
    }

    void testUnitTestWithObjectId() {
        mockDomain(Sample)

        def s = new Sample(name: 'Sample1')
        s.save()
        assert s.id != null

        s = Sample.get(s.id)
        assert s != null
    }
}

@Entity
class Sample {

    static mapWith = 'mongo'

    ObjectId id
    Long version

    String name
}
