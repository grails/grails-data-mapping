package org.grails.orm.hibernate

import grails.persistence.Entity
import org.grails.orm.hibernate.cfg.GrailsHibernateUtil

import org.junit.Test
import static junit.framework.Assert.*

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class HasPropertyAndRespondsToWithProxyTests extends AbstractGrailsHibernateTests {

    @Test
    void testHasPropertyWithProxy() {

        def rt = RespondsTo.newInstance(name:"Bob")
        def hp = HasProperty.newInstance(one:rt)
        assertNotNull "should have saved", rt.save()
        assertNotNull "should have saved", hp.save()

        session.clear()

        hp = HasProperty.get(1)
        assertFalse 'should be a proxy!', GrailsHibernateUtil.isInitialized(hp, "one")

        def proxy = GrailsHibernateUtil.getAssociationProxy(hp, "one")
        assertNotNull "should have a name property!", proxy.hasProperty("name")
    }

    @Test
    void testRespondsToWithProxy() {

        def rt = RespondsTo.newInstance(name:"Bob")
        def hp = HasProperty.newInstance(one:rt)
        assertNotNull "should have saved", rt.save()
        assertNotNull "should have saved", hp.save()

        session.clear()

        hp = HasProperty.get(1)
        assertFalse 'should be a proxy!', GrailsHibernateUtil.isInitialized(hp, "one")

        def proxy = GrailsHibernateUtil.getAssociationProxy(hp, "one")

        assertNotNull "should have a foo method!", proxy.respondsTo("foo")
        assertNotNull "should have a bar method!", proxy.respondsTo("bar", String)
    }

    @Override
    protected getDomainClasses() {
        [HasProperty, RespondsTo, SubclassRespondsTo]
    }
}

@Entity
class HasProperty {
    Long id
    Long version

    RespondsTo one
}

@Entity
class RespondsTo {
    Long id
    Long version

    String name
}

@Entity
class SubclassRespondsTo extends RespondsTo {
    String name
    def foo() { "good" }
    def bar(String i) { i }
}
