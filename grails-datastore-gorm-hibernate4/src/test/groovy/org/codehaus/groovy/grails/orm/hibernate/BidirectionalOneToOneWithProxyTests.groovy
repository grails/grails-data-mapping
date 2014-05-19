package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test

import static junit.framework.Assert.*
/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Jun 9, 2009
 */
class BidirectionalOneToOneWithProxyTests extends AbstractGrailsHibernateTests {


    // test for GRAILS-4580
    @Test
    void testBidirectionalOneToOneWithProxy() {
        def nose = new BidirectionalOneToOneWithProxyNose(length:2)
        def face = new BidirectionalOneToOneWithProxyFace(width:10, height:8, nose:nose)
        assertNotNull "should have saved face",face.save(flush:true)

        assertEquals 1, BidirectionalOneToOneWithProxyNose.count()
        session.clear()

        def faces = BidirectionalOneToOneWithProxyFace.list()
        face = faces[0]
        nose = face.nose
        nose.length = 3

        assertNotNull "saving nose should have been successful", nose.save()
    }

    @Override
    protected getDomainClasses() {
        [BidirectionalOneToOneWithProxyFace, BidirectionalOneToOneWithProxyNose]
    }
}

@Entity
class BidirectionalOneToOneWithProxyFace {
    Long id
    Long version

    Integer height
    Integer width
    BidirectionalOneToOneWithProxyNose nose
}

@Entity
class BidirectionalOneToOneWithProxyNose {
    Long id
    Long version

    Integer length
    BidirectionalOneToOneWithProxyFace face
    static belongsTo = [face:BidirectionalOneToOneWithProxyFace]
}

