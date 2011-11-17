package org.grails.datastore.test

/**
 * Tests that the session is correctly shared across all operations
 */
import grails.datastore.test.DatastoreUnitTestMixin
import grails.persistence.Entity

@Mixin(DatastoreUnitTestMixin)
class SessionLifeCycleTests extends GroovyTestCase {

    protected void tearDown() {
        disconnect()
    }

    void testSample() {
        mockDomain(Sample)
        def s = new Sample(name: "Larry")
        s.save()

        assert Sample.count() == 1
        assertTrue !Sample.list().isEmpty()
    }

    void testSampleMockNoSave() {
        def s = new Sample(name: "Larry")
        mockDomain(Sample, [s])

        assertTrue !Sample.list().isEmpty()
    }
}

@Entity
class Sample {
    Long id
    String name

}
