package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test

import static junit.framework.Assert.*

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Jan 22, 2009
 */
class CompositeIdentifierWithAssociationsTests extends AbstractGrailsHibernateTests {

    @Test
    void testCompositeWithBidirectionalOneToOne() {
        def parent = CompositeIdentifierWithAssociationsParent.newInstance()

        parent.col1 = "one"
        parent.col2 = "two"
        parent.address = CompositeIdentifierWithAssociationsAddress.newInstance(postCode:"32984739", parent:parent)

        assertNotNull "should have saved parent", parent.save(flush:true)

        session.clear()

        parent = CompositeIdentifierWithAssociationsParent.get(CompositeIdentifierWithAssociationsParent.newInstance(col1:"one", col2:"two"))

        assertEquals "32984739", parent.address.postCode
    }

    @Test
    void testCompositeIdentiferWithBidirectionalOneToMany() {
        def parent = CompositeIdentifierWithAssociationsParent.newInstance()

        parent.col1 = "one"
        parent.col2 = "two"

        def child = CompositeIdentifierWithAssociationsChild.newInstance()
        parent.addToChildren(child)
        assertNotNull "should have saved", parent.save(flush:true)

        session.clear()

        parent = CompositeIdentifierWithAssociationsParent.get(CompositeIdentifierWithAssociationsParent.newInstance(col1:"one", col2:"two"))
        assertEquals 1, parent.children.size()

        session.clear()

        child = CompositeIdentifierWithAssociationsChild.get(1)
        assertEquals "one", child.parent.col1
        assertEquals "two", child.parent.col2
    }

    @Test
    void testCompositeIdentiferWithUnidirectionalOneToMany() {

        def parent = CompositeIdentifierWithAssociationsParent.newInstance()
        parent.col1 = "one"
        parent.col2 = "two"

        def child = CompositeIdentifierWithAssociationsChild2.newInstance()
        parent.addToChildren2(child)
        assertNotNull "should have saved", parent.save(flush:true)

        session.clear()

        parent = CompositeIdentifierWithAssociationsParent.get(CompositeIdentifierWithAssociationsParent.newInstance(col1:"one", col2:"two"))
        assertEquals 1, parent.children2.size()
    }

    @Override
    protected getDomainClasses() {
        [CompositeIdentifierWithAssociationsParent, CompositeIdentifierWithAssociationsChild, CompositeIdentifierWithAssociationsChild2, CompositeIdentifierWithAssociationsAddress]
    }
}

trait SomeTrait {
    void foo(){}
}

@Entity
class CompositeIdentifierWithAssociationsParent implements Serializable, SomeTrait {
    Long version

    Set children
    Set children2
    static hasMany = [children: CompositeIdentifierWithAssociationsChild,
                      children2: CompositeIdentifierWithAssociationsChild2]
    String col1
    String col2
    CompositeIdentifierWithAssociationsAddress address

    static mapping = { id composite:['col1', 'col2'] }
    static constraints = { address nullable:true }
}

@Entity
class CompositeIdentifierWithAssociationsChild {
    Long id
    Long version

    CompositeIdentifierWithAssociationsParent parent
    static belongsTo = [parent : CompositeIdentifierWithAssociationsParent]
}

@Entity
class CompositeIdentifierWithAssociationsChild2 {
    Long id
    Long version

}

@Entity
class CompositeIdentifierWithAssociationsAddress {
    Long id
    Long version

    String postCode
    CompositeIdentifierWithAssociationsParent parent
    static belongsTo = [parent:CompositeIdentifierWithAssociationsParent]
}
