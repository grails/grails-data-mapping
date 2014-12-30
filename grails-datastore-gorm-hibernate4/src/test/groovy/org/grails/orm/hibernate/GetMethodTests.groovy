package org.grails.orm.hibernate

import grails.persistence.Entity

import org.junit.Test

import static junit.framework.Assert.*

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: May 23, 2008
 */
class GetMethodTests extends AbstractGrailsHibernateTests {


    @Test
    void testGetMethod() {
        assertNull GetMethodTest.get(null)
        assertNull GetMethodTest.get(1)
        assertNull GetMethodTest.get(1L)

        assertNotNull GetMethodTest.newInstance(name:"Foo").save(flush: true)

        assertNotNull GetMethodTest.get(1)
    }

    @Test
    void testReadMethod() {
        assertNull GetMethodTest.read(null)
        assertNull GetMethodTest.read(1)
        assertNull GetMethodTest.read(1L)

        assertNotNull GetMethodTest.newInstance(name:"Foo").save(flush: true)

        assertNotNull GetMethodTest.read(1L)

        def test = GetMethodTest.read(1)
        test.name = "Bar"

        session.flush()
        session.clear()

        test = GetMethodTest.read(1)
        assertEquals "Foo", test.name
    }

    @Test
    void testGetMethodZeroId() {
        assertNull GetMethodZeroIdTest.get(null)
        assertNull GetMethodZeroIdTest.get(0)
        assertNull GetMethodZeroIdTest.get(0L)

        def zeroInstance = GetMethodZeroIdTest.newInstance()
        zeroInstance.id = 0
        assertNotNull zeroInstance.save(flush: true)

        assertNotNull GetMethodZeroIdTest.get(0)
    }

    @Override
    protected getDomainClasses() {
        [GetMethodTest, GetMethodZeroIdTest]
    }
}

@Entity
class GetMethodTest {
    Long id
    Long version
    String name
}

@Entity
class GetMethodZeroIdTest {
    Long id
    Long version
    static mapping = {
        id generator:'assigned'
    }
}
