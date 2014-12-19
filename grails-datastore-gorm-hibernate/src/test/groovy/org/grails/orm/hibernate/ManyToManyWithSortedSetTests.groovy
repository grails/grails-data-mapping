package org.grails.orm.hibernate

import static junit.framework.Assert.*
import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Nov 19, 2007
 */
class ManyToManyWithSortedSetTests extends AbstractGrailsHibernateTests {

    @Test
    void testManyToManyWithSortedSet() {
        def bar1 = ManyToManyWithSortedSetBar.newInstance(name:'Bar 1', sortOrder:1)
        assertNotNull bar1.save()
        def bar2 = ManyToManyWithSortedSetBar.newInstance(name:'Bar 2', sortOrder:2)
        assertNotNull bar2.save()

        def foo = ManyToManyWithSortedSetFoo.newInstance()
        foo.addToBars(bar2)
        foo.addToBars(bar1)
        assertNotNull foo.save()

        session.flush()

        session.clear()

        foo = ManyToManyWithSortedSetFoo.get(1)
        assertNotNull foo
        assertEquals "Bar 1", foo.bars.first().name
        assertEquals "Bar 2", foo.bars.last().name
    }

    @Override
    protected getDomainClasses() {
        [ManyToManyWithSortedSetFoo, ManyToManyWithSortedSetBar]
    }
}

class ManyToManyWithSortedSetFoo {
    Long id
    Long version
    SortedSet bars

    static hasMany = [bars:ManyToManyWithSortedSetBar]
}

class ManyToManyWithSortedSetBar implements Comparable {
    String name
    int sortOrder

    Long id
    Long version
    Set foos
    static belongsTo = ManyToManyWithSortedSetFoo
    static hasMany = [foos:ManyToManyWithSortedSetFoo]

    int compareTo(that) {
        sortOrder <=> that.sortOrder
    }
}
