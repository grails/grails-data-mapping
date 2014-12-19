package org.grails.orm.hibernate

import org.junit.Test
import static junit.framework.Assert.*

/**
 * @author Graeme Rocher
 */
class IdentityNameMappingTests extends AbstractGrailsHibernateTests {

    @Test
    void testIdentityNameMapping() {
        def test = IdentityNameMapping.newInstance(test: "John")

        assertNotNull "Persistent instance with named and assigned identifier should have validated", test.save(flush: true)

        session.clear()

        assertNotNull "Persistent instance with named and assigned identifier should have been saved", IdentityNameMapping.get("John")
    }

    @Override
    protected getDomainClasses() {
        [IdentityNameMapping]
    }
}
class IdentityNameMapping {
    Long id
    Long version
    String test

    static mapping = {
        id name:'test', generator:'assigned'
    }
}