package org.grails.orm.hibernate

import grails.persistence.Entity

import java.sql.ResultSet

import org.junit.Test

import static junit.framework.Assert.*

/**
 * @author Joshua Burnett
 * @since 1.2.1
 */
class DiscriminatorFormulaMappingTests extends AbstractGrailsHibernateTests {

    @Test
    void testDiscriminatorMapping() {
        //save with 1
        assertNotNull "should have saved root", DiscriminatorFormulaMappingRoot.newInstance().save(flush:true)
        //save with 9
        def root = DiscriminatorFormulaMappingRoot.newInstance()
        root.tree = 9
        assertNotNull "should have saved root 9", root.save(flush:true)
        root = DiscriminatorFormulaMappingRoot.newInstance()
        root.tree = 2 //save a child2 from root
        assertNotNull "should have saved child2", root.save(flush:true)

        session.clear()

        assertEquals 3, DiscriminatorFormulaMappingRoot.list().size()

        def child2 = DiscriminatorFormulaMappingChild2.newInstance()
        child2.tree = 22
        assertNotNull "should have saved child2", child2.save(flush:true)

        def child3 = DiscriminatorFormulaMappingChild3.newInstance(name:"josh")

        assertNotNull "should have saved child3", child3.save(flush:true)

        session.clear()

        assertEquals 5, DiscriminatorFormulaMappingRoot.list().size()
        session.clear()

        assertEquals 3, DiscriminatorFormulaMappingChild2.list().size()
        assertEquals 1, DiscriminatorFormulaMappingChild3.list().size()

        def conn = session.connection()

        ResultSet rs = conn.prepareStatement("select tree from discriminator_formula_mapping_root").executeQuery()
        rs.next()
        assertEquals null, rs.getString("tree")
        rs.next()
        assertEquals 9, rs.getInt("tree")
        rs.next()
        assertEquals 2, rs.getInt("tree")
        rs.next()
        assertEquals 22, rs.getInt("tree")
        rs.next()
        assertEquals 3, rs.getInt("tree")

        rs.close()
    }

    //test Work around for http://opensource.atlassian.com/projects/hibernate/browse/HHH-2855
    @Test
    void testDiscriminatorMapping_HHH_2855() {
        def owner = DiscriminatorFormulaMappingOwner.newInstance()
        owner.addToChildList(DiscriminatorFormulaMappingChild3.newInstance(name:"Bob"))
        owner.addToChild2List(DiscriminatorFormulaMappingChild2.newInstance())

        assertNotNull "should have saved instance", owner.save(flush:true)

        session.clear()

        def a = DiscriminatorFormulaMappingOwner.get(1)

        def children = a.childList
        assertEquals 1, children.size()
        def children2 = a.child2List
        assertEquals 2, children2.size() //this will end with 2 because child3 extend child2
    }

    @Override
    protected getDomainClasses() {
        [DiscriminatorFormulaMappingRoot, DiscriminatorFormulaMappingChild2, DiscriminatorFormulaMappingChild3, DiscriminatorFormulaMappingOwner]
    }
}


@Entity
class DiscriminatorFormulaMappingRoot {
    Long id
    Long version

    Integer tree
    static mapping = {
        discriminator value:"1", formula:'case when tree in (1, 9) or tree is null then 1 when tree in (2, 22) then 2 else tree end', type:"integer"
    }
    static constraints = {
        tree(nullable:true)
    }
}

@Entity
class DiscriminatorFormulaMappingChild2 extends DiscriminatorFormulaMappingRoot {
    DiscriminatorFormulaMappingOwner owner
    static mapping = {
        discriminator "2"
    }
    static constraints = {
        owner(nullable:true)
    }
    def beforeInsert = {
        tree = tree?:2
    }
}

@Entity
class DiscriminatorFormulaMappingChild3 extends DiscriminatorFormulaMappingChild2 {
    String name

    static mapping = {
        discriminator "3"
    }
    def beforeInsert = {
        tree = tree?:3
    }
}

@Entity
class DiscriminatorFormulaMappingOwner {
    Long id
    Long version

    static constraints = {}

    Set childList, child2List
    static hasMany = [childList:DiscriminatorFormulaMappingChild3,child2List:DiscriminatorFormulaMappingChild2]
}
