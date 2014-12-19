package org.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test

import static junit.framework.Assert.*

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Nov 14, 2007
 */
class ListReorderingTests extends AbstractGrailsHibernateTests {

    @Test
    void testReorderList() {
        def foo = ListReorderingFoo.newInstance(name:"foo")
                          .addToBars(name:"bar1")
                          .addToBars(name:"bar2")

        assertEquals foo,foo.bars[0].foo
        assertEquals foo,foo.bars[1].foo

        foo.save()

        session.flush()
        session.clear()

        foo = ListReorderingFoo.get(1)
        assertNotNull foo
        assertEquals 2, foo.bars.size()
        assertEquals "bar1", foo.bars[0].name
        assertEquals "bar2", foo.bars[1].name

        def tmp = foo.bars[0]
        foo.bars[0] = foo.bars[1]
        foo.bars[1] = tmp

        session.flush()
        session.clear()

        foo = ListReorderingFoo.get(1)
        assertEquals 2, foo.bars.size()

        assertEquals "bar2", foo.bars[0].name
        assertEquals "bar1", foo.bars[1].name
    }

    @Override
    protected getDomainClasses() {
        [ListReorderingBar, ListReorderingFoo]
    }
}


@Entity
class ListReorderingBar {
    Long id
    Long version

    String name
    ListReorderingFoo foo
    static belongsTo = ListReorderingFoo
}

@Entity
class ListReorderingFoo {
    Long id
    Long version

    String name

    List bars
    static hasMany = [bars : ListReorderingBar]
}
