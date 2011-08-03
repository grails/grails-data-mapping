package org.grails.datastore.gorm.mongo

import grails.datastore.test.DatastoreUnitTestMixin
import org.bson.types.ObjectId
import grails.persistence.Entity
/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 8/3/11
 * Time: 1:06 PM
 * To change this template use File | Settings | File Templates.
 */
@Mixin(DatastoreUnitTestMixin)
class UnitTestWithObjectIdTests extends GroovyTestCase{

    protected void setUp() {
        super.setUp()

        connect()
    }

    protected void tearDown() {
        disconnect()

        super.tearDown()
    }


    void testSomething() {
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

    static constraints = {
    }
}
