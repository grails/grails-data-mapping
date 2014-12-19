package org.grails.orm.hibernate

import org.junit.Test

import static junit.framework.Assert.*

/**
 * @author Graeme Rocher
 */
class EmbeddedConstraintsTests extends AbstractGrailsHibernateTests {

    @Test
    void testEmbeddedCascadingValidation() {
        def cust = EmbeddedConstraintsCustomer.newInstance(name:"Fred")

        assertFalse cust.validate()

        cust.headOffice = EmbeddedConstraintsAddress.newInstance()
        cust.deliverySite = EmbeddedConstraintsAddress.newInstance()

        assertFalse cust.validate()

        cust.headOffice.street = "22"
        cust.deliverySite.street = "47"
        cust.headOffice.postcode = "34334"
        cust.deliverySite.postcode = "33343"

        assertTrue cust.validate()
    }

    @Override
    protected getDomainClasses() {
        [EmbeddedConstraintsCustomer]
    }
}
class EmbeddedConstraintsCustomer {
    Long id
    Long version
    String name

    EmbeddedConstraintsAddress headOffice
    EmbeddedConstraintsAddress deliverySite

    static embedded = ['headOffice', 'deliverySite']
}

class EmbeddedConstraintsAddress {
    String street
    String postcode
    String other

    static transients = ['other']

    static constraints = {
        street(matches:/\d+/)
        postcode(nullable: true)
    }
}
