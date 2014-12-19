package org.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class BidirectionalOneToOneNoOwnerTests extends AbstractGrailsHibernateTests {

    @Test
    void testBidirectionalOneToOneWithNoOwnerIsActuallyUnidirectional() {
        assert "should have saved", new BidirectionalOneToOneNoOwnerProfile(firstName:"one", lastName:"two").save(flush:true)
    }
}
@Entity
class BidirectionalOneToOneNoOwnerUser {
    Long id
    Long version

    String username
    BidirectionalOneToOneNoOwnerProfile profile

    static constraints = {
        profile(nullable:true)
    }
}

@Entity
class BidirectionalOneToOneNoOwnerProfile {
    Long id
    Long version

    String firstName
    String lastName
    BidirectionalOneToOneNoOwnerUser adminInCharge

    static constraints = {
        adminInCharge(nullable:true)
    }
}