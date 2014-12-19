package org.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test

import static junit.framework.Assert.*

/**
 * Tests the isDirty and getPersistentValue methods.
 *
 * @author Burt Beckwith
 */
class DirtyTests extends AbstractGrailsHibernateTests {

    @Test
    void testIsDirty() {

        def d = Dirt.newInstance(pr1: 'pr1', pr2: new Date(), pr3: 123)
        d.save(flush: true, failOnError: true)
        session.clear()

        d = Dirt.get(d.id)
        assertNotNull d

        assertFalse d.isDirty('pr1')
        assertFalse d.isDirty('pr2')
        assertFalse d.isDirty('pr3')
        assertFalse d.isDirty()

        d.pr1 = d.pr1.reverse()
        assertTrue d.isDirty('pr1')
        assertFalse d.isDirty('pr2')
        assertFalse d.isDirty('pr3')
        assertTrue d.isDirty()

        d.pr1 = d.pr1.reverse()
        d.pr2++
        assertFalse d.isDirty('pr1')
        assertTrue d.isDirty('pr2')
        assertFalse d.isDirty('pr3')
        assertTrue d.isDirty()

        d.pr2--
        d.pr3++
        assertFalse d.isDirty('pr1')
        assertFalse d.isDirty('pr2')
        assertTrue d.isDirty('pr3')
        assertTrue d.isDirty()
    }

    @Test
    void testGetDirtyPropertyNames() {

        def d = Dirt.newInstance(pr1: 'pr1', pr2: new Date(), pr3: 123)
        d.save(flush: true, failOnError: true)
        session.clear()

        d = Dirt.get(d.id)
        assertNotNull d

        assertFalse d.isDirty()
        assertEquals 0, d.dirtyPropertyNames.size()

        d.pr1 = d.pr1.reverse()
        assertTrue d.isDirty()
        assertEquals(['pr1'], d.dirtyPropertyNames)

        d.pr2++
        assertTrue d.isDirty()
        assertEquals(['pr1', 'pr2'], d.dirtyPropertyNames.sort())

        d.pr3++
        assertTrue d.isDirty()
        assertEquals(['pr1', 'pr2', 'pr3'], d.dirtyPropertyNames.sort())
    }

    @Test
    void testGetPersistentValue() {

        String pr1 = 'pr1'
        Date pr2 = new Date()
        int pr3 = 123

        def d = Dirt.newInstance(pr1: pr1, pr2: pr2, pr3: pr3)
        d.save(flush: true, failOnError: true)
        session.clear()

        d = Dirt.get(d.id)
        assertNotNull d

        d.pr1.reverse()
        d.pr2++
        d.pr3++

        assertEquals pr1, d.getPersistentValue('pr1')
        assertEquals pr2.time, d.getPersistentValue('pr2').time
        assertEquals pr3, d.getPersistentValue('pr3')
    }

    void testGetPersistentValueRead() {

        String pr1 = 'pr1'
        Date pr2 = new Date()
        int pr3 = 123

        def d = Dirt.newInstance(pr1: pr1, pr2: pr2, pr3: pr3)
        d.save(flush: true, failOnError: true)
        session.clear()

        d = Dirt.read(d.id)
        assertNotNull d

        d.pr1.reverse()
        d.pr2++
        d.pr3++

        assertNull d.getPersistentValue('pr1')
        assertNull d.getPersistentValue('pr2')
        assertNull d.getPersistentValue('pr3')
    }

    void testNewInstances() {

        def d = DirtWithValidator.newInstance(pr1: 'pr1', pr2: new Date(), pr3: 123)
        assertFalse d.isDirty()
        assertFalse d.isDirty('pr1')
        assertFalse d.isDirty('pr2')
        assertFalse d.isDirty('pr3')
        assertEquals 0, d.getDirtyPropertyNames().size()

        d.save(flush: true, failOnError: true)
        session.clear()

        assertNotNull DirtWithValidator.get(d.id)
    }

    @Override
    protected getDomainClasses() {
        [Dirt, DirtWithValidator]
    }
}

@Entity
class Dirt {

    Long id
    Long version

    String pr1
    Date pr2
    Integer pr3
}

@Entity
class DirtWithValidator {
    Long id
    Long version

    String pr1
    Date pr2
    Integer pr3

    static constraints = {
        pr1(validator: { val, obj -> if (obj.isDirty('pr3')) {} })
    }
}

