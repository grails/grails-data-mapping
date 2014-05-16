package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.junit.Test

import static junit.framework.Assert.*
/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Jan 23, 2009
 */
class BidirectionalOneToManyAndUnidirectionalOneToManyTests extends AbstractGrailsHibernateTests {

    @Override
    protected getDomainClasses() {
        [BidirectionalOneToManyAndUnidirectionalOneToManyGroup, BidirectionalOneToManyAndUnidirectionalOneToManyUser]
    }

    @Test
    void testDomain() {
        GrailsDomainClass groupDomain = ga.getDomainClass(BidirectionalOneToManyAndUnidirectionalOneToManyGroup.name)
        GrailsDomainClass userDomain = ga.getDomainClass(BidirectionalOneToManyAndUnidirectionalOneToManyUser.name)

        assertTrue "property [members] should be a one-to-many",groupDomain.getPropertyByName("members").isOneToMany()
        assertFalse "property [members] should be a unidirectional",groupDomain.getPropertyByName("members").isBidirectional()

        assertTrue "property [owner] should be a many-to-one",groupDomain.getPropertyByName("owner").isManyToOne()
        assertTrue "property [owner] should be bidirectional",groupDomain.getPropertyByName("owner").isBidirectional()

        assertTrue "property [groups] should be a one-to-many", userDomain.getPropertyByName("groups").isOneToMany()
        assertTrue "property [groups] should be a bidirectional", userDomain.getPropertyByName("groups").isBidirectional()
    }
}

@Entity
class BidirectionalOneToManyAndUnidirectionalOneToManyGroup {

    Long id
    Long version

    // Every user has a bunch of groups that it owns
    BidirectionalOneToManyAndUnidirectionalOneToManyUser owner
    static belongsTo = [ owner : BidirectionalOneToManyAndUnidirectionalOneToManyUser ]

    // In addition, every group has members that are users
    Set members
    static hasMany = [ members : BidirectionalOneToManyAndUnidirectionalOneToManyUser ]
}

@Entity
class BidirectionalOneToManyAndUnidirectionalOneToManyUser {
    Long id
    Long version

    // Every user has a bunch of groups that it owns
    Set groups
    static hasMany = [ groups: BidirectionalOneToManyAndUnidirectionalOneToManyGroup ]
    static mappedBy = [ groups:"owner" ]
}