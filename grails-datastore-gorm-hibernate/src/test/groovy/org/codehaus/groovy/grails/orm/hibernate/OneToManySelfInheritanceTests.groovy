package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

import static junit.framework.Assert.*
import org.junit.Test

/**
 * @author Joshua Burnett
 * @since 1.2.1
 */
class OneToManySelfInheritanceTests extends AbstractGrailsHibernateTests {


    @Test
    void testOneToManyWithSelf() {
        def org1 = Org.newInstance(name:"org1")
        assertNotNull "should have saved",org1.save(flush:true)
        def orga = Org.newInstance(name:"orga",parent: org1)
        assertNotNull "should have saved",orga.save(flush:true)
        def orgb = Org.newInstance(name:"orgb",parent: org1)
        assertNotNull "should have saved",orgb.save(flush:true)
        def orgGrandChild = Org.newInstance(name:"orggrand",parent: orga)
        assertNotNull "should have saved",orgGrandChild.save(flush:true)

        session.clear()

        assertEquals 4, Org.list().size()

        session.clear()
        def o = Org.get(1)
        assertEquals 2, o.children.size()
        def oa = Org.findByName("orga")
        assertEquals 1, oa.children.size()

        def ogrand = Org.findByName("orggrand")
        assertEquals 0, ogrand.children.size()
        assertEquals oa, ogrand.parent
    }

    @Test
    void testOneToManyExt() {

        def org1 = ExtOrg.newInstance(name:"org1")
        assertNotNull "should have saved",org1.save(flush:true)
        def orga = ExtOrg.newInstance(name:"orga",parent: org1)
        assertNotNull "should have saved",orga.save(flush:true)
        def orgb = ExtOrg.newInstance(name:"orgb",parent: org1)
        assertNotNull "should have saved",orgb.save(flush:true)
        def orgGrandChild = ExtOrg.newInstance(name:"orggrand",parent: orga)
        assertNotNull "should have saved",orgGrandChild.save(flush:true)

        session.clear()

        assertEquals 4, ExtOrg.list().size()

        session.clear()
        def o = ExtOrg.get(1)
        assertEquals 2, o.children.size()
        def oa = ExtOrg.findByName("orga")
        assertEquals 1, oa.children.size()

        def ogrand = ExtOrg.findByName("orggrand")
        assertEquals 0, ogrand.children.size()
        assertEquals oa, ogrand.parent
    }

    @Override
    protected getDomainClasses() {
        [OrgRoot, Org, ExtOrg]
    }
}

@Entity
class OrgRoot {
    Long id
    Long version

}

@Entity
class Org extends OrgRoot {
    String name
    Set children
    static hasMany = [children: Org]
    Org parent
    static constraints = {
        parent(nullable: true)
    }
}

@Entity
class ExtOrg extends Org {}
