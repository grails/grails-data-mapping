package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

import java.sql.Connection

import org.junit.Test
import static junit.framework.Assert.*

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class HasOneMappingTests extends AbstractGrailsHibernateTests {

    @Override
    protected getDomainClasses() {
        [HasOneNose, HasOneFace]
    }

    @Test
    void testHasOneMapping() {
        def nose = HasOneNose.newInstance(shape: "round")
        def f = HasOneFace.newInstance(name:"Bob", nose: nose)
        nose.face = f

        assertNotNull "entities should be associated",f.nose
        assertNotNull "entities should be associated",f.nose.face
        f.save(flush:true)

        session.clear()

        f = HasOneFace.get(1)
        assertNotNull "should have been able to read back nose",f.nose

        // now test table structure
        Connection c = session.connection()
        def r = c.prepareStatement("select * from has_one_face").executeQuery()
        r.next()
        r.getLong("id")
        r.getLong("version")
        r.getString("name")
        shouldFail {
            r.getLong("face_id")
        }

        r = c.prepareStatement("select * from has_one_nose").executeQuery()
        r.next()
        r.getLong("id")
        r.getLong("version")
        r.getString("shape")
        r.getLong("face_id") // association key stored in child

        // now test delete
        f.delete(flush:true)

        assertEquals 0, HasOneFace.count()
        assertEquals 0, HasOneNose.count()
    }
}


@Entity
class HasOneFace {
    Long id
    Long version

    String name
    HasOneNose nose
    static hasOne = [nose:HasOneNose]
}

@Entity
class HasOneNose {
    Long id
    Long version

    String shape
    HasOneFace face
}
