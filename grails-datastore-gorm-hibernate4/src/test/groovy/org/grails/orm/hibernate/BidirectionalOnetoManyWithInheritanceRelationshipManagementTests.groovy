package org.grails.orm.hibernate

import grails.core.GrailsDomainClass
import grails.persistence.Entity

import org.junit.Test

import static junit.framework.Assert.*
/**
 * Longest class name in history!
 *
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Mar 13, 2008
 */
class BidirectionalOnetoManyWithInheritanceRelationshipManagementTests extends AbstractGrailsHibernateTests {


    @Test
    void testRelationshipManagementMethods() {
        GrailsDomainClass manySideClass = ga.getDomainClass(BidirectionalOnetoManyManySide.name)
        GrailsDomainClass oneSideClass = ga.getDomainClass(BidirectionalOnetoManyOneSide.name)
        GrailsDomainClass subManySideClass = ga.getDomainClass(BidirectionalOnetoManySubManySide.name)

        def collection = subManySideClass.getPropertyByName("oneSides")
        assert collection

        assertTrue collection.isBidirectional()
        def otherSide = collection.getOtherSide()
        assert otherSide

        def manySide = manySideClass.newInstance()
        def oneSide1 = oneSideClass.newInstance()

        manySide.addToOneSides(oneSide1)
        manySide.save(flush:true) // OK
        assertEquals "1", 1, manySide.oneSides?.size() // OK
        assertNotNull "2", oneSide1.manySide // OK
        assertEquals "3", oneSide1.manySide?.id, manySide.id // OK

        def subManySide = subManySideClass.newInstance()
        def oneSide2 = oneSideClass.newInstance()

        subManySide.addToOneSides(oneSide2)
        assertEquals "4", 1, subManySide.oneSides?.size() // OK
        assertNotNull "5", oneSide2.manySide // NG
        assertEquals "6", oneSide2.manySide?.id, subManySide.id
        subManySide.save(flush:true)
    }

    @Override
    protected getDomainClasses() {
        [BidirectionalOnetoManyManySide, BidirectionalOnetoManySubManySide, BidirectionalOnetoManyOneSide]
    }
}
@Entity
class BidirectionalOnetoManyManySide {
    Long id
    Long version
    Set oneSides
    static hasMany = [oneSides:BidirectionalOnetoManyOneSide]
}

@Entity
class BidirectionalOnetoManySubManySide extends BidirectionalOnetoManyManySide {}

@Entity
class BidirectionalOnetoManyOneSide {
    Long id
    Long version
    BidirectionalOnetoManyManySide manySide
    static belongsTo = [manySide:BidirectionalOnetoManyManySide]
}