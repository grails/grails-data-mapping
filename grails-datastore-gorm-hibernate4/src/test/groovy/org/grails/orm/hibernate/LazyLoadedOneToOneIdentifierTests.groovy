package org.grails.orm.hibernate

import org.grails.orm.hibernate.cfg.GrailsHibernateUtil
import org.grails.core.util.ClassPropertyFetcher
import org.hibernate.Hibernate
import org.hibernate.proxy.HibernateProxy


import static junit.framework.Assert.*
import org.junit.Test

/**
* @author Graeme Rocher
* @since 1.0
*
* Created: Mar 14, 2008
*/
class LazyLoadedOneToOneIdentifierTests extends AbstractGrailsHibernateTests {

    @Test
	void testDynamicMethodOnProxiedObject() {
        def cpf = ClassPropertyFetcher.forClass(LazyLoadedUserIdentifier)
		def user = LazyLoadedUser.newInstance(name:"Fred")

		assertNotNull user.save(flush:true)

		def id = LazyLoadedUserIdentifier.newInstance(user:user)
		assertNotNull id.save(flush:true)

		session.clear()

		id = LazyLoadedUserIdentifier.get(1)
        def proxy = cpf.getPropertyValue(id, "user")
		assertTrue "should be a hibernate proxy", (proxy instanceof HibernateProxy)
		assertFalse "proxy should not be initialized", Hibernate.isInitialized(proxy)
		assertNotNull "calling save() on the proxy should have worked",proxy.save()
	}

    @Test
	void testMethodCallsOnProxiedObjects() {
        def cpf = ClassPropertyFetcher.forClass(LazyLoadedUserIdentifier)
		def user = LazyLoadedUser.newInstance(name:"Fred")
		assertNotNull user.save(flush:true)

		def id = LazyLoadedUserIdentifier.newInstance(user:user)
		assertNotNull id.save(flush:true)

		session.clear()

		id = LazyLoadedUserIdentifier.get(1)

        def proxy = cpf.getPropertyValue(id, "user")
		assertTrue "should be a hibernate proxy", (proxy instanceof HibernateProxy)
		assertFalse "proxy should not be initialized", Hibernate.isInitialized(proxy)
		assertEquals "Fred", proxy.name
	}

    @Test
	void testObtainIdFromLazyLoadedObject() {
		def user = LazyLoadedUser.newInstance(name:"Fred")
		assertNotNull user.save(flush:true)

		def id = LazyLoadedUserIdentifier.newInstance(user:user)
		assertNotNull id.save(flush:true)

		session.clear()

		id = LazyLoadedUserIdentifier.get(1)

		assertFalse "user should have been lazy loaded", GrailsHibernateUtil.isInitialized(id, "user")

		def dbId = id.userId
		assertEquals 1, dbId
		assertFalse "accessed identifier, but lazy association should not have been initialized", GrailsHibernateUtil.isInitialized(id, "user")
	}

    @Override
    protected getDomainClasses() {
        [LazyLoadedUserIdentifier, LazyLoadedUser]
    }
}

class LazyLoadedUserIdentifier {
    Long id
    Long version
    LazyLoadedUser user
    static belongsTo = [user:LazyLoadedUser]
    static mapping = { user lazy:true }
}

class LazyLoadedUser {
    Long id
    Long version
    String name
}
