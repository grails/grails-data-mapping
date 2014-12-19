package org.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test

class AllDeleteOrphanMappingTests extends AbstractGrailsHibernateTests {

    // test for GRAILS-6734
    @Test
    void testDeleteOrphanMapping() {
        def a1 = new AllDeleteOrphanMappingA(val: "A1")
        def d1 = new AllDeleteOrphanMappingD(val: "D1")
        d1.addToAyes(a1)
        d1.save()

        def a2 = new AllDeleteOrphanMappingA(val: "A2")
        def d2 = new AllDeleteOrphanMappingD(val: "D2")
        d2.addToAyes(a2)
        d2.save(flush: true)

        session.clear()

        d2 = AllDeleteOrphanMappingD.get(2)

        // Initialize d2, then detach it
        d2.ayes.each { it.bees.iterator() }
        d2.discard()

        d2.addToAyes(AllDeleteOrphanMappingA.get(1))
        d2 = d2.merge(flush:true)
    }

    @Override
    protected getDomainClasses() {
        [AllDeleteOrphanMappingA, AllDeleteOrphanMappingB, AllDeleteOrphanMappingD]
    }
}

@Entity
class AllDeleteOrphanMappingA {
    Long id
    Long version
    static mapping = {
        bees cascade: "all-delete-orphan"
    }

    String val
    Set bees
    AllDeleteOrphanMappingD parent
    static hasMany = [bees: AllDeleteOrphanMappingB]
    static belongsTo = [parent: AllDeleteOrphanMappingD]
}

@Entity
class AllDeleteOrphanMappingB {
    Long id
    Long version

    String val
    AllDeleteOrphanMappingA parent
    static belongsTo = [parent: AllDeleteOrphanMappingA]
}

@Entity
class AllDeleteOrphanMappingD {
    Long id
    Long version

    String val
    Set ayes
    static hasMany = [ayes: AllDeleteOrphanMappingA]
}