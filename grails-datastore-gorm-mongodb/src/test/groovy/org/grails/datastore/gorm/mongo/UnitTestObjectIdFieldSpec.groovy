package org.grails.datastore.gorm.mongo

import grails.datastore.test.DatastoreUnitTestMixin
import grails.persistence.Entity
import org.bson.types.ObjectId
import spock.lang.Issue
import spock.lang.Specification

/**
 * @author Graeme Rocher
 */
@Mixin(DatastoreUnitTestMixin)
class UnitTestObjectIdFieldSpec extends Specification {

    void setup() {
        ExpandoMetaClass.enableGlobally()
        connect()
    }

    void cleanup() {
        ExpandoMetaClass.disableGlobally()
        disconnect()
    }

    @Issue('GPMONGODB-172')
    void "Test storing and retrieving an entity with an ObjectId field in unit tests"() {
       when:"A entity is mocked and a query by id executed"
            mockDomain(Test)

            final oid = ObjectId.get()
            new Test(someOtherId: oid).save(flush:true)
            session.clear()
            def t = Test.findBySomeOtherId(oid)

        then:"No error is thrown and the correct entity is returned"
            t != null
            t.someOtherId == oid

    }
}

@Entity
class Test {

    ObjectId id
    Long version
    ObjectId someOtherId

    static constraints = {
    }
}
