package org.grails.orm.hibernate

import static junit.framework.Assert.*
import org.junit.Test

import grails.persistence.Entity

import org.grails.orm.hibernate.cfg.GrailsHibernateUtil
import org.hibernate.Hibernate
import org.hibernate.proxy.HibernateProxy

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Feb 27, 2009
 */
class InheritanceWithLazyProxiesTests extends AbstractGrailsHibernateTests {

    @Test
    void testLazyAssociationsWithInheritance() {

        def attrb = new InheritanceWithLazyProxiesAttributeB()
        attrb.save()

        def b = new InheritanceWithLazyProxiesB(type:attrb, attr:attrb).save(flush:true)

        assertNotNull "subclass should have been saved", b

        session.clear()

        b = InheritanceWithLazyProxiesB.get(1)
        def type = GrailsHibernateUtil.getAssociationProxy(b, "type")

        assertFalse "should not have been initialized",GrailsHibernateUtil.isInitialized(b, "type")
        b.discard()

        assertNotNull "dynamic finder should have worked with proxy", InheritanceWithLazyProxiesB.findByType(type)
        session.clear()
        assertFalse "should not have been initialized",Hibernate.isInitialized(type)
        assertNotNull "dynamic finder should have worked with proxy", InheritanceWithLazyProxiesA.findByAttr(type)
    }

    @Test
    void testInstanceOfMethod() {

        def attrb = new InheritanceWithLazyProxiesAttributeB()
        attrb.save()

        def b = new InheritanceWithLazyProxiesB(type:attrb, attr:attrb).save(flush:true)

        assertNotNull "subclass should have been saved", b
        session.clear()

        b = InheritanceWithLazyProxiesB.get(1)

        def type = GrailsHibernateUtil.getAssociationProxy(b, "type")

        assertFalse "should not have been initialized",GrailsHibernateUtil.isInitialized(b, "type")
        assertTrue "should be a hibernate proxy", (type instanceof HibernateProxy)

        assertTrue "instanceOf method should have returned true",type.instanceOf(InheritanceWithLazyProxiesAttributeA)
        assertTrue "instanceOf method should have returned true",type.instanceOf(InheritanceWithLazyProxiesAttributeB)
    }

    @Override
    protected getDomainClasses() {
        [InheritanceWithLazyProxiesA,InheritanceWithLazyProxiesB, InheritanceWithLazyProxiesAttributeA, InheritanceWithLazyProxiesAttributeB]
    }
}

@Entity
class InheritanceWithLazyProxiesA {
    Long id
    Long version

    InheritanceWithLazyProxiesAttributeB attr
    static belongsTo = [attr:InheritanceWithLazyProxiesAttributeB]
}

@Entity
class InheritanceWithLazyProxiesB extends InheritanceWithLazyProxiesA {

    InheritanceWithLazyProxiesAttributeA type
    static belongsTo = [type:InheritanceWithLazyProxiesAttributeA]
}

@Entity
class InheritanceWithLazyProxiesAttributeA {
    Long id
    Long version

}

@Entity
class InheritanceWithLazyProxiesAttributeB extends InheritanceWithLazyProxiesAttributeA {}
