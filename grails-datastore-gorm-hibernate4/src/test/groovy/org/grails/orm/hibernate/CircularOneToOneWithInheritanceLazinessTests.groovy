package org.grails.orm.hibernate

import grails.persistence.Entity
import org.grails.orm.hibernate.cfg.GrailsHibernateUtil

import org.junit.Test

import static junit.framework.Assert.*

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class CircularOneToOneWithInheritanceLazinessTests extends AbstractGrailsHibernateTests {

    @Test
    void testForLazyProxies() {

        def c = CircularOneToOneWithInheritanceLazinessContent.newInstance().save(flush:true)
        assertNotNull "should have saved c", c

        def vc = CircularOneToOneWithInheritanceLazinessVirtualContent.newInstance(target:c).save(flush:true)
        assertNotNull "should have saved vc", vc

        session.clear()

        def c1 = CircularOneToOneWithInheritanceLazinessVirtualContent.get(2)
        assertTrue "should not be a proxy!", GrailsHibernateUtil.isInitialized(c1, "target")
    }

    @Override
    protected getDomainClasses() {
        [CircularOneToOneWithInheritanceLazinessContent,CircularOneToOneWithInheritanceLazinessVirtualContent ]
    }
}

@Entity
class CircularOneToOneWithInheritanceLazinessContent {
    Long id
    Long version

    static mapping = {}
}

@Entity
class CircularOneToOneWithInheritanceLazinessVirtualContent extends CircularOneToOneWithInheritanceLazinessContent {
    Long id
    Long version

    CircularOneToOneWithInheritanceLazinessContent target

    static mapping = {
        target lazy:false
    }
}
