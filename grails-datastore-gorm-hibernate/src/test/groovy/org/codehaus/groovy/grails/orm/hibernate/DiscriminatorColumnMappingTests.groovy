package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test

import java.sql.ResultSet

import static junit.framework.Assert.*

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class DiscriminatorColumnMappingTests extends AbstractGrailsHibernateTests {

    @Test
    void testDiscriminatorMapping() {
        assertNotNull "should have saved root", DiscriminatorColumnMappingRoot.newInstance().save(flush:true)

        def conn = session.connection()
        ResultSet rs = conn.prepareStatement("select tree from discriminator_column_mapping_root").executeQuery()
        rs.next()
        assertEquals 1, rs.getInt("tree")

        rs.close()

        assertNotNull "should have saved child1", DiscriminatorColumnMappingChild1.newInstance().save(flush:true)

        rs = conn.prepareStatement("select tree from discriminator_column_mapping_root").executeQuery()
        rs.next()
        assertEquals 1, rs.getInt("tree")

        rs.next()
        assertEquals 2, rs.getInt("tree")
    }

    @Override
    protected getDomainClasses() {
        [DiscriminatorColumnMappingRoot, DiscriminatorColumnMappingChild1, DiscriminatorColumnMappingChild2]
    }
}


@Entity
class DiscriminatorColumnMappingRoot {

    Long id
    Long version

    static mapping = {
        discriminator value:"1", column:[name:"tree",sqlType:"int"]
    }
}

@Entity
class DiscriminatorColumnMappingChild1 extends DiscriminatorColumnMappingRoot {

    static mapping = {
        discriminator "2"
    }
}

@Entity
class DiscriminatorColumnMappingChild2 extends DiscriminatorColumnMappingRoot {

    static mapping = {
        discriminator "3"
    }
}
