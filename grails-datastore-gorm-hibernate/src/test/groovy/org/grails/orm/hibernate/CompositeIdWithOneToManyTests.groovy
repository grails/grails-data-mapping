package org.grails.orm.hibernate

import grails.persistence.Entity;

import org.junit.Test

import static junit.framework.Assert.*

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Nov 19, 2007
 */
class CompositeIdWithOneToManyTests extends AbstractGrailsHibernateTests {

    @Test
    void testCompositeIdWithOneToMany() {

        def left = Left.newInstance()
        left.addToCenters(Center.newInstance(foo:"bar1"))
            .addToCenters(Center.newInstance(foo:"bar2"))

        left.save()

        session.flush()
        session.clear()

        left = Left.get(1)

        assertNotNull left
        assertEquals 2, left.centers.size()

        def c1  = Center.get(Center.newInstance(foo:"bar1", left:left))
        assertNotNull c1
        assertEquals "bar1",c1.foo
    }

    @Override
    protected getDomainClasses() {
        [Left, Center]
    }
}
@Entity
class Left {
    Long id
    Long version
    Set centers
    static hasMany = [centers:Center]
}

@Entity
class Center implements Serializable {
    Long id
    Long version
    Left left
    String foo
    static belongsTo = [ left:Left ]
    static mapping = {
        id composite:['left', 'foo']
    }
}
