package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

import static junit.framework.Assert.*
import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Jan 22, 2009
 */
class ManyToManyWithListTests extends AbstractGrailsHibernateTests {

    @Test
    void testManyToManyWithList() {
        def one = ManyToManyWithListFoo.newInstance()
        one.addToBars(ManyToManyWithListBar.newInstance())
        assertNotNull "should have saved",one.save(flush:true)

        session.clear()

        one = ManyToManyWithListFoo.get(1)
        assertEquals 1, one.bars.size()
        assertNotNull one.bars[0]

        session.clear()

        def two = ManyToManyWithListBar.get(1)
        assertEquals 1, two.parents.size()
    }

    @Override
    protected getDomainClasses() {
        [ManyToManyWithListFoo, ManyToManyWithListBar]
    }
}


@Entity
class ManyToManyWithListFoo {
    Long id
    Long version

    static hasMany = [bars:ManyToManyWithListBar  ]
    List bars
}

@Entity
class ManyToManyWithListBar {
    Long id
    Long version

    Set parents
    static belongsTo = ManyToManyWithListFoo
    static hasMany = [parents:ManyToManyWithListFoo]
}
