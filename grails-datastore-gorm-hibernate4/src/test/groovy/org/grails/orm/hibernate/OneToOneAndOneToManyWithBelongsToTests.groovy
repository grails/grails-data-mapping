package org.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test

class OneToOneAndOneToManyWithBelongsToTests extends AbstractGrailsHibernateTests {

    @Test
    void testOneToOneAndOneToManyWithBelongsTo() {
        def user = UserBoth.newInstance(name: 'foo')
        def address = AddressBoth.newInstance(street: 'billing', user:user)
        user.billingAddress = address
        user.addToBusinessLocations(street:"location")

        assert user.save(flush:true) != null
    }

    @Override
    protected getDomainClasses() {
        [UserBoth,AddressBoth]
    }
}

@Entity
class AddressBoth {
    Long id
    Long version

    String street
    UserBoth user
    static belongsTo=[user: UserBoth]
}

@Entity
class UserBoth {
    Long id
    Long version

    String name
    AddressBoth billingAddress
    static hasOne = [billingAddress:AddressBoth ]
    Set businessLocations
    static hasMany = [businessLocations : AddressBoth]
    static mappedBy = [billingAddress:'user']
}
