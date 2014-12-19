package org.grails.orm.hibernate

import grails.persistence.Entity
import org.hibernate.proxy.HibernateProxy
import static junit.framework.Assert.*
import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class UnwrapProxyInDynamicFinderTests extends AbstractGrailsHibernateTests {


    @Test
    void testReturnNonProxiedInstanceInFinder() {
        def status = UnwrapProxyInDynamicFinderProjectStatus.newInstance(name:"signed", description:"foo")

        assertNotNull "should have saved", status.save(flush:true)
        assertNotNull "should have saved", UnwrapProxyInDynamicFinderProject.newInstance(name:"foo", projectStatus:status).save(flush:true)

        session.clear()

        def project = UnwrapProxyInDynamicFinderProject.get(1)
        assertEquals "signed", project.projectStatus.name
        assertFalse "Should not return proxy from finder!", UnwrapProxyInDynamicFinderProjectStatus.signed instanceof HibernateProxy
        assertFalse "Should not return proxy from finder!", UnwrapProxyInDynamicFinderProjectStatus.get(status.id) instanceof HibernateProxy
        assertFalse "Should not return proxy from finder!", UnwrapProxyInDynamicFinderProjectStatus.read(status.id) instanceof HibernateProxy
        assertFalse "Should not return proxy from finder!",
            UnwrapProxyInDynamicFinderProjectStatus.find("from UnwrapProxyInDynamicFinderProjectStatus as p where p.name='signed'") instanceof HibernateProxy
        assertFalse "Should not return proxy from finder!", UnwrapProxyInDynamicFinderProjectStatus.findWhere(name:'signed') instanceof HibernateProxy

        def c = UnwrapProxyInDynamicFinderProjectStatus.createCriteria()
        def result = c.get {
            eq 'name', 'signed'
        }

        assertFalse "Should not return proxy from criteria!", result instanceof HibernateProxy
    }

    @Override
    protected getDomainClasses() {
        [UnwrapProxyInDynamicFinderProject, UnwrapProxyInDynamicFinderProjectStatus]
    }
}

@Entity
class UnwrapProxyInDynamicFinderProject {

    Long id
    Long version

    String name
    UnwrapProxyInDynamicFinderProjectStatus  projectStatus
}

@Entity
class UnwrapProxyInDynamicFinderProjectStatus {

    Long id
    Long version

    String name
    String description

    static getSigned() {
        UnwrapProxyInDynamicFinderProjectStatus.findByName("signed")
    }
}
