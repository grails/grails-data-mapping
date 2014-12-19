package org.grails.orm.hibernate

import grails.core.GrailsDomainClass
import grails.persistence.Entity
import org.junit.Test

import static junit.framework.Assert.*

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Jan 22, 2009
 */
class ManyToManyWithMapTests extends AbstractGrailsHibernateTests {

    @Test
    void testManyToManyWithMapDomain() {
        GrailsDomainClass fooClass = ga.getDomainClass(ManyToManyWithMapFoo.name)
        GrailsDomainClass barClass= ga.getDomainClass(ManyToManyWithMapBar.name)

        assertTrue "should be many-to-many",fooClass.getPropertyByName("bars").isManyToMany()
        assertTrue "should be an association",fooClass.getPropertyByName("bars").isAssociation()

        assertTrue "should be many-to-many",barClass.getPropertyByName("parents").isManyToMany()
        assertTrue "should be an association",barClass.getPropertyByName("parents").isAssociation()
    }

    @Test
    void testManyToManyWithMap() {
        def foo = ManyToManyWithMapFoo.newInstance()
        foo.bars['bar1'] = ManyToManyWithMapBar.newInstance()

        assertNotNull "should have saved",foo.save(flush:true)

        session.clear()

        foo = ManyToManyWithMapFoo.get(1)
        assertEquals 1, foo.bars.size()
        assertNotNull foo.bars['bar1']

        session.clear()

        def bar = ManyToManyWithMapBar.get(1)
        assertEquals 1, bar.parents.size()
    }

    @Override
    protected getDomainClasses() {
        [ManyToManyWithMapFoo,ManyToManyWithMapBar]
    }
}


@Entity
class ManyToManyWithMapFoo {
    Long id
    Long version

    static hasMany = [bars:ManyToManyWithMapBar]
    Map bars = new HashMap()
}

@Entity
class ManyToManyWithMapBar {
    Long id
    Long version
    Set parents
    static belongsTo = ManyToManyWithMapFoo
    static hasMany = [parents:ManyToManyWithMapFoo]
}
