package org.grails.orm.hibernate

import grails.core.GrailsDomainClass
import org.junit.Test

import static junit.framework.Assert.*
/**
 * @author Graeme Rocher
 * @since 0.4
 */
class CascadingDeleteBehaviour2Tests extends AbstractGrailsHibernateTests {

    @Test
    void testCascadingDeleteFromChild() {
        def u = new CascadingDeleteBehaviourUserRecord()
        u.name = "bob"
        u.save(flush:true)

        def i = new CascadingDeleteBehaviourItem()
        i.name = "stuff"
        i.save(flush:true)

        def ir = new CascadingDeleteBehaviourItemRating()

        ir.user = u
        ir.item = i
        ir.rating = 5
        ir.save(flush:true)

        session.clear()

        ir = CascadingDeleteBehaviourItemRating.get(1)
        ir.delete()
        session.flush()

        assertEquals 1, CascadingDeleteBehaviourUserRecord.count()
        assertEquals 1, CascadingDeleteBehaviourItem.count()
        assertEquals 0, CascadingDeleteBehaviourItemRating.count()
    }

    @Test
    void testDomainModel() {
        GrailsDomainClass ir = ga.getDomainClass(CascadingDeleteBehaviourItemRating.name)
        GrailsDomainClass uClass = ga.getDomainClass(CascadingDeleteBehaviourUserRecord.name)
        GrailsDomainClass iClass = ga.getDomainClass(CascadingDeleteBehaviourItem.name)

        assertTrue ir.isOwningClass(uClass.clazz)
        assertTrue ir.isOwningClass(iClass.clazz)
        assertFalse uClass.isOwningClass(ir.clazz)
        assertFalse iClass.isOwningClass(ir.clazz)
    }

    @Override
    protected getDomainClasses() {
        [CascadingDeleteBehaviourUserRecord, CascadingDeleteBehaviourItem, CascadingDeleteBehaviourItemRating]
    }
}

class CascadingDeleteBehaviourUserRecord {
    Long id
    Long version
    static hasMany = [ratings:CascadingDeleteBehaviourItemRating]

    Set ratings
    String name
}

class CascadingDeleteBehaviourItem {
    Long id
    Long version
    static hasMany = [ratings:CascadingDeleteBehaviourItemRating]

    Set ratings
    String name
}

class CascadingDeleteBehaviourItemRating {
    Long id
    Long version
    static belongsTo = [CascadingDeleteBehaviourUserRecord,CascadingDeleteBehaviourItem]

    CascadingDeleteBehaviourUserRecord user
    CascadingDeleteBehaviourItem item
    int rating
}