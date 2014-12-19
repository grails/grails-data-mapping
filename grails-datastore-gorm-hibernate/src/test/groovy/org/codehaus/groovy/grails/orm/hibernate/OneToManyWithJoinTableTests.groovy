package org.codehaus.groovy.grails.orm.hibernate

import static junit.framework.Assert.*
import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Oct 4, 2007
 */
class OneToManyWithJoinTableTests extends AbstractGrailsHibernateTests {

    @Test
    void testOneToManyJoinTableMapping() {
        def g = ThingGroup.newInstance()

        g.name = "Group 1"
        def t1 = Thing.newInstance()
        t1.name = "Bob"
        g.addToThings(t1)
        g.save()

        session.flush()
        session.clear()

        g = ThingGroup.get(1)

        def t = Thing.newInstance()
        t.name = "Fred"
        g.addToThings(t)
        g.save()

        session.flush()
        session.clear()

        g = ThingGroup.get(1)
        assertEquals 2, g.things.size()
    }

    @Override
    protected getDomainClasses() {
        [Thing, ThingGroup]
    }
}

class Thing {
    Long id
    Long version
    String name
}

class ThingGroup {
    Long id
    Long version
    Set things
    Set moreThings
    static hasMany = [things: Thing, moreThings:Thing]
    static mapping = {
        columns {
            things joinTable:true
            moreThings joinTable:'more_things'
        }
    }
    String name
}
