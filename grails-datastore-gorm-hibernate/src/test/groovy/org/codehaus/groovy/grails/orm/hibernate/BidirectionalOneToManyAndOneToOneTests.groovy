package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test

/**
 * @author graemerocher
 */
class BidirectionalOneToManyAndOneToOneTests extends AbstractGrailsHibernateTests{


    @Test
    void testSaveAndLoad() {
        session.enableFilter("dynamicFilterEnabler")

        def user = new BidirectionalOneToManyAndOneToOneUser()
        user.name = 'Pete'
        user.membership = new BidirectionalOneToManyAndOneToOneMembership(user: user, dateCreated: new Date())
        user.save(failOnError : true)

        user = BidirectionalOneToManyAndOneToOneUser.findByName('Pete')
        user.save(failOnError : true)
        session.flush()
        session.clear()

        user = BidirectionalOneToManyAndOneToOneUser.findByName('Pete')
    }

    @Override
    protected getDomainClasses() {
        [BidirectionalOneToManyAndOneToOneMembership, BidirectionalOneToManyAndOneToOneUser]
    }
}

@Entity
class BidirectionalOneToManyAndOneToOneMembership{
    Long id
    Long version

    String a = 'b'
    Set referrals
    BidirectionalOneToManyAndOneToOneUser user
    Date dateCreated
    static hasMany = [ referrals : BidirectionalOneToManyAndOneToOneUser ]
    static belongsTo = [ user: BidirectionalOneToManyAndOneToOneUser ]

}

@Entity
class BidirectionalOneToManyAndOneToOneUser{
    Long id
    Long version

    String name
    BidirectionalOneToManyAndOneToOneMembership membership

    static mappedBy = [membership:"user"]
}