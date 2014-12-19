package org.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test

import static junit.framework.Assert.*

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class ChildWithAssociationAndDiscriminatorTests extends AbstractGrailsHibernateTests {


    @Test
    void testChildObjectWithAssociationAndCustomDiscriminator() {

        def associatedInstance = new ChildWithAssociationAndDiscriminatorAssociated()
        associatedInstance.addToChildList( new ChildWithAssociationAndDiscriminatorChild())

        assertNotNull "should have saved instance", associatedInstance.save(flush:true)
        session.clear()

        def a = ChildWithAssociationAndDiscriminatorAssociated.get(1)
        def children = a.childList
        assertNotNull "should have had some children", children
        assertTrue "should have had some children", children.size() > 0

        def child2 = new ChildWithAssociationAndDiscriminatorChild2(name:"bob")

        assertNotNull "should be able to save second child", child2.save(flush:true)?.id
    }

    @Override
    protected getDomainClasses() {
        [ChildWithAssociationAndDiscriminatorParent, ChildWithAssociationAndDiscriminatorChild, ChildWithAssociationAndDiscriminatorChild2, ChildWithAssociationAndDiscriminatorAssociated]
    }
}

@Entity
class ChildWithAssociationAndDiscriminatorParent {

    Long id
    Long version
    static mapping = {
        discriminator column:[name:"test_discriminator",sqlType:"varchar"], value:"custom_parent"
    }
}

@Entity
class ChildWithAssociationAndDiscriminatorChild extends ChildWithAssociationAndDiscriminatorParent {

    static mapping = {
        discriminator "custom_child"
    }

    static belongsTo = [myObject:ChildWithAssociationAndDiscriminatorAssociated]
    ChildWithAssociationAndDiscriminatorAssociated myObject
}

@Entity
class ChildWithAssociationAndDiscriminatorChild2 {
    Long id
    Long version

    static mapping = {
        discriminator "custom_child"
    }

    String name
}

@Entity
class ChildWithAssociationAndDiscriminatorAssociated {
    Long id
    Long version

    Set childList
    static hasMany = [childList:ChildWithAssociationAndDiscriminatorChild]
}
