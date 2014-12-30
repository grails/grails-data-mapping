package org.grails.orm.hibernate

import grails.core.GrailsDomainClass
import grails.persistence.Entity

import org.junit.Test

import static junit.framework.Assert.*


/**
 * @author graemerocher
 */
class CyclicManyToManyWithInheritanceTests extends AbstractGrailsHibernateTests {

    @Test
    void testDomain() {
        GrailsDomainClass individualDomain = ga.getDomainClass(CyclicManyToManyWithInheritanceIndividual.name)
        GrailsDomainClass userDomain = ga.getDomainClass(CyclicManyToManyWithInheritanceUser.name)
        def userGroupDomain = ga.getDomainClass(CyclicManyToManyWithInheritanceUserGroup.name)

        assertTrue "should be a many-to-many assocation",userGroupDomain.getPropertyByName("members").isManyToMany()
        assertTrue "should be a many-to-many assocation",userDomain.getPropertyByName("groups").isManyToMany()
        assertTrue "should be a many-to-many assocation",individualDomain.getPropertyByName("groups").isManyToMany()
    }

    @Test
    void testCyclicManyToManyWithInheritance() {

        def wallace = CyclicManyToManyWithInheritanceIndividual.newInstance(name: "Wallace")
        def gromit = CyclicManyToManyWithInheritanceIndividual.newInstance(name: "Gromit")
        def cheeseLovers = CyclicManyToManyWithInheritanceUserGroup.newInstance(name: "Cheese Lovers")
        def cooker = CyclicManyToManyWithInheritanceIndividual.newInstance(name: "Cooker")
        def lunaphiles = CyclicManyToManyWithInheritanceUserGroup.newInstance(name: "Lunaphiles")
        wallace.addToGroups(cheeseLovers)
        gromit.addToGroups(cheeseLovers)
        cooker.addToGroups(lunaphiles)
        // here's the line that causes the problem
        cheeseLovers.addToGroups(lunaphiles)
        wallace.save()
        gromit.save()
        cooker.save()
    }

    @Override
    protected getDomainClasses() {
        [CyclicManyToManyWithInheritanceUser, CyclicManyToManyWithInheritanceUserGroup, CyclicManyToManyWithInheritanceIndividual]
    }
}

@Entity
class CyclicManyToManyWithInheritanceIndividual extends CyclicManyToManyWithInheritanceUser {}

@Entity
class CyclicManyToManyWithInheritanceUser {

    Long id
    Long version

    String name

    Set groups
    static hasMany = [ groups: CyclicManyToManyWithInheritanceUserGroup ]
}

@Entity
class CyclicManyToManyWithInheritanceUserGroup extends CyclicManyToManyWithInheritanceUser {

    static belongsTo = [CyclicManyToManyWithInheritanceIndividual,
                        CyclicManyToManyWithInheritanceUser,
                        CyclicManyToManyWithInheritanceUserGroup]

    Set members
    static hasMany = [ members: CyclicManyToManyWithInheritanceUser ]
}
