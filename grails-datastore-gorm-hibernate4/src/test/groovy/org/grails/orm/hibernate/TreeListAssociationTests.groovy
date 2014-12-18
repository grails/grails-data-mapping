package org.codehaus.groovy.grails.orm.hibernate

import static junit.framework.Assert.*
import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class TreeListAssociationTests extends AbstractGrailsHibernateTests {


    @Test
    void testTreeListAssociation() {
        def root = TreeListCustomer.newInstance(description:"root")

        assertNotNull "should have saved root", root.save(flush:true)

        root.addToChildren(description:"child1")
            .addToChildren(description:"child2")
            .save(flush:true)

        session.clear()

        root = TreeListCustomer.get(1)
        assertEquals "child1",root.children[0].description
        assertEquals "child2",root.children[1].description

        def one = root.children[0]
        def two = root.children[1]
        root.children[0] = two
        root.children[1] = one
        root.save(flush:true)

        session.clear()

        root = TreeListCustomer.get(1)

        assertEquals "child2", root.children[0].description
        assertEquals "child1", root.children[1].description
    }

    @Override
    protected getDomainClasses() {
        [TreeListCustomer]
    }
}


class TreeListCustomer {

    Long id
    Long version

    TreeListCustomer parent
    List children
    String description

    static belongsTo = [parent:TreeListCustomer]
    static hasMany = [children:TreeListCustomer]

    static constraints = {
        parent(nullable: true)
        children(nullable: true)
    }
}
