package org.codehaus.groovy.grails.orm.hibernate

import org.junit.Test

import static junit.framework.Assert.*

class ClosureMappingTests extends AbstractGrailsHibernateTests {

    @Test
    void testClosureMapping() {
        def thing = new ClosureMappingThing()
        assertEquals "Hello, Fred!", thing.whoHello("Fred")
    }

    @Override
    protected getDomainClasses() {
        [ClosureMappingThing]
    }
}
class ClosureMappingThing {
    Long id
    Long version
    String name

    def whoHello = { who -> "Hello, ${who}!" }
}
