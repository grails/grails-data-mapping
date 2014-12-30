package org.grails.datastore.test

import grails.datastore.test.DatastoreUnitTestMixin
import grails.persistence.Entity
import spock.lang.Specification

/**
 * @author graemerocher
 */
class DatastoreUnitTestCaseSpec extends Specification {

    // NOTE: we run the same test twice to ensure setup/teardown is cleaning up properly
    void "Test mock domain 1"() {
        given:
            TestTests tt = new TestTests()
            tt.metaClass.mixin DatastoreUnitTestMixin

        when:
            tt.setUp()
            tt.testCRUD()
            tt.tearDown()

        then:
            true == true
    }

    void "Test mock domain 2"() {
        given:
            TestTests tt = new TestTests()

            tt.metaClass.mixin DatastoreUnitTestMixin

        when:
            tt.setUp()
            tt.testCRUD()
            tt.tearDown()

        then:
            true == true
    }
}

@Mixin(DatastoreUnitTestMixin)
class TestTests extends GroovyTestCase {

    protected void setUp() {
        super.setUp()
        connect()
    }

    protected void tearDown() {
        super.tearDown()
        disconnect()
    }

    void testCRUD() {
        mockDomain TestDomain

        def t = new TestDomain(name:"Bob")
        t.save()

        assert t.id != null

        t = TestDomain.get(t.id)

        assert t != null
    }
}

@Entity
class TestDomain {
    Long id
    Long version
    String name
}
