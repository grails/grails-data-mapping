package org.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test

import static junit.framework.Assert.*

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class MappingInheritanceTests extends AbstractGrailsHibernateTests {


    @Test
    void testMappingInheritance() {

        def c1 = MappingInheritanceChild1.newInstance(active:true, someField:"foo").save(flush:true)
        assertNotNull "should have saved Child1", c1

        def c2 = MappingInheritanceChild2.newInstance(active:false, anotherBoolean:true).save(flush:true)
        assertNotNull "should have saved Child1", c2

        def conn = session.connection()
        def rs = conn.prepareStatement("SELECT active, another_boolean FROM Mapping_Inheritance_PARENT").executeQuery()
        rs.next()
        assertEquals "Y", rs.getString("active")
        rs.next()
        assertEquals "N", rs.getString("active")
        assertEquals "Y", rs.getString("another_boolean")
    }

    @Override
    protected getDomainClasses() {
        [MappingInheritanceParent, MappingInheritanceChild1, MappingInheritanceChild2]
    }
}


@Entity
class MappingInheritanceParent {
    Long id
    Long version

    Boolean active
    static mapping = {
        active type: 'yes_no'
    }
}

@Entity
class MappingInheritanceChild1 extends MappingInheritanceParent {
    String someField
}

@Entity
class MappingInheritanceChild2 extends MappingInheritanceParent {
    boolean anotherBoolean

    static mapping = {
        anotherBoolean type:"yes_no"
    }
}
