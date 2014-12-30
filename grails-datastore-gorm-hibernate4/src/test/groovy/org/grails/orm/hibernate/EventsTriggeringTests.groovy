package org.grails.orm.hibernate

import grails.persistence.Entity

import org.junit.Test

import static junit.framework.Assert.*
/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Oct 13, 2008
 */
class EventsTriggeringTests extends AbstractGrailsHibernateTests {

    @Test
    void testEvents() {

        def test = EventsTriggering.newInstance(name:"Foo")
        def testData = test.eventData

        assertNull testData.beforeInsert
        assertNull testData.afterInsert
        assertNull testData.afterUpdate
        assertNull testData.beforeUpdate
        assertNull testData.beforeDelete
        assertNull testData.afterDelete
        assertNull testData.beforeLoad
        assertNull testData.afterLoad

        assertNotNull test.save(flush:true)

        assertTrue session.contains(test)
        assertTrue testData.beforeInsert
        assertTrue testData.afterInsert
        assertNull testData.afterUpdate
        assertNull testData.beforeUpdate
        assertNull testData.beforeDelete
        assertNull testData.afterDelete
        assertNull testData.beforeLoad
        assertNull testData.afterLoad

        test.name = "Bar"
        assertNotNull test.save(flush:true)

        assertTrue testData.beforeInsert
        assertTrue testData.afterInsert
        assertTrue testData.afterUpdate
        assertTrue testData.beforeUpdate
        assertNull testData.beforeDelete
        assertNull testData.afterDelete
        assertNull testData.beforeLoad
        assertNull testData.afterLoad

        session.clear()

        test = EventsTriggering.get(1)
        testData = test.eventData

        assertNull testData.beforeInsert
        assertNull testData.afterInsert
        assertNull testData.afterUpdate
        assertNull testData.beforeUpdate
        assertNull testData.beforeDelete
        assertNull testData.afterDelete
        assertTrue testData.beforeLoad
        assertTrue testData.afterLoad

        test.delete(flush:true)

        assertNull testData.beforeInsert
        assertNull testData.afterInsert
        assertNull testData.afterUpdate
        assertNull testData.beforeUpdate
        assertTrue testData.beforeDelete
        assertTrue testData.afterDelete
        assertTrue testData.beforeLoad
        assertTrue testData.afterLoad
    }

    @Override
    protected getDomainClasses() {
        [EventsTriggering]
    }
}

@Entity
class EventsTriggering {

    Long id
    Long version

    String name

    def eventData = [:]

    static def beforeInsert = {
        eventData['beforeInsert'] = true
    }

    def afterInsert = {
        eventData['afterInsert'] = true
    }

    def beforeUpdate = {
        eventData['beforeUpdate'] = true
    }

    def afterUpdate = {
        eventData['afterUpdate'] = true
    }

    def beforeLoad = {
        eventData['beforeLoad'] = true
    }

    def afterLoad = {
        eventData['afterLoad'] = true
    }

    def beforeDelete = {
        eventData['beforeDelete'] = true
    }

    def afterDelete = {
        eventData['afterDelete'] = true
    }
}