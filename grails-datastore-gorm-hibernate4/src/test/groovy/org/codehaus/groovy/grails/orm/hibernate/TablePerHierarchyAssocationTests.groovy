package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

import static junit.framework.Assert.*
import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Mar 18, 2008
 */
class TablePerHierarchyAssocationTests extends AbstractGrailsHibernateTests {


    @Test
    void testLoadSubclassAssociation() {

        def test =  TablePerHierarchOneToMany.newInstance()
                             .addToSubs(name:"one")
                             .addToSubs(name:"two")
                             .addToRoots(name:"three")

        test.addToRoots(TablePerHierarchSub2.newInstance(name:"four"))
        test.addToRoots(TablePerHierarchyRoot.newInstance(name:"five"))

        assertNotNull test.save(flush:true)

        session.clear()

        test = TablePerHierarchOneToMany.get(1)
        assertEquals 2, test.subs.size()
        assertEquals 5, test.roots.size()
    }

    @Override
    protected getDomainClasses() {
        [TablePerHierarchyRoot, TablePerHierarchSub1, TablePerHierarchSub2, TablePerHierarchOneToMany]
    }
}

@Entity
class  TablePerHierarchyRoot {
    Long id
    Long version

    String name
    TablePerHierarchOneToMany one
}
@Entity
class TablePerHierarchSub1 extends TablePerHierarchyRoot {}
@Entity
class TablePerHierarchSub2 extends TablePerHierarchyRoot {}

@Entity
class TablePerHierarchOneToMany {

    Long id
    Long version
    Set subs, roots
    static hasMany = [subs:TablePerHierarchSub1, roots:TablePerHierarchyRoot]
}
