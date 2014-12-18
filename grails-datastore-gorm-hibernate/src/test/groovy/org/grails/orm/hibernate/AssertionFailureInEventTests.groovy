package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test
import spock.lang.Issue

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class AssertionFailureInEventTests extends AbstractGrailsHibernateTests {

    protected getDomainClasses() {
        [AssertionParent, AssertionChild, AssertionSubChild]
    }

    // test for HHH-2763 and GRAILS-4453
    @Issue('GRAILS-4453')
    @Test
    void testNoAssertionErrorInEvent() {
        def p = new AssertionParent().save()
        p.addToChilds(new AssertionChild(s:"one"))
        p.save(flush:true)

        session.clear()

        p = AssertionParent.findById(1, [fetch:[childs:'join']])

        p.addToChilds(new AssertionChild(s:"two"))
        p.save(flush:true)

        session.clear()

        p = AssertionParent.get(1)
        p.childs.each { println it.s }
    }
}

@Entity
class AssertionParent {
    Long id
    Long version
    Set childs
    static hasMany = [childs : AssertionChild]

    def beforeUpdate = {
        calc()
    }

    def calc = {
        childs.each { it.s = "change" }
    }
}

@Entity
class AssertionChild {
    Long id
    Long version
    Set subChilds

    static belongsTo = [AssertionParent]
    static hasMany = [ subChilds : AssertionSubChild ]
    String s
}

@Entity
class AssertionSubChild {
    Long id
    Long version

    static belongsTo = [AssertionChild]
    String s
}
