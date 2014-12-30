package org.grails.orm.hibernate

import grails.persistence.Entity;

import org.junit.Test

import static junit.framework.Assert.*

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Mar 13, 2008
 */
class CompositeIdentityUpdateTests extends AbstractGrailsHibernateTests {

    @Test
    void testUpdateObjectWithCompositeId() {
        def t = CompositeIdentityUpdateT.newInstance(x:"1", y:"2", name:"John")

        assertNotNull t.save(flush:true)

        session.clear()

        t = CompositeIdentityUpdateT.get(CompositeIdentityUpdateT.newInstance(x:"1", y:"2"))
        assertNotNull t

        t.name = "Fred"
        t.save(flush:true)

        session.clear()

        t = CompositeIdentityUpdateT.get(CompositeIdentityUpdateT.newInstance(x:"1", y:"2"))
        assertEquals "Fred", t.name
    }

    @Override
    protected getDomainClasses() {
        [CompositeIdentityUpdateT]
    }
}

@Entity
class CompositeIdentityUpdateT implements Serializable {
    Long id
    Long version
    String x
    String y
    String name
    static mapping = {
        id composite:['x', 'y']
    }
}
