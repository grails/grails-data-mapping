package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.DuplicateKeyException

import static junit.framework.Assert.*
import org.junit.Test

/**
 * @author grocher
 */
class OneToOneTests extends AbstractGrailsHibernateTests {

    @Test
    void testPersistAssociation() {

        def nose = OneToOneNose.newInstance()
        def face = OneToOneFace.newInstance(nose:nose)
        nose.face = face

        assertNotNull face.nose
        assertNotNull nose.face

        assertNotNull face.save(flush:true)

        session.clear()

        def newFace = OneToOneFace.get(1)
        def newNose = OneToOneNose.get(1)

        assertNotNull newFace.nose
        assertNotNull newNose.face

        def differentFace = OneToOneFace.newInstance(nose:newNose)

        shouldFail(DuplicateKeyException) {
            differentFace.save(flush:true)
        }
    }

    @Test
    void testOneToOneTableStructure() {
        def conn = session.connection()
        conn.prepareStatement("select nose_id from one_to_one_face").execute()
        shouldFail {
            conn.prepareStatement("select face_id from one_to_one_face").execute()
        }

        // only the owner should have the foreign key
        shouldFail {
            conn.prepareStatement("select face_id from one_to_one_nose").execute()
        }
    }

    @Override
    protected getDomainClasses() {
        [OneToOneFace, OneToOneNose]
    }
}


@Entity
class OneToOneFace {
    Long id
    Long version

    OneToOneNose nose

    static mapping = {
        nose unique:true
    }
}

@Entity
class OneToOneNose {
    Long id
    Long version

    OneToOneFace face
    static belongsTo = [face: OneToOneFace]
}
