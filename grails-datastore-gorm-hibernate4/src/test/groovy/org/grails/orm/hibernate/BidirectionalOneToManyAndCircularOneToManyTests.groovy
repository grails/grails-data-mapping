package org.grails.orm.hibernate

import grails.core.GrailsDomainClass

import static junit.framework.Assert.*
import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Apr 9, 2008
 */
class BidirectionalOneToManyAndCircularOneToManyTests extends AbstractGrailsHibernateTests {

    @Test
    void testBidirectionalOneToManyAndCircularOneToMany() {
        GrailsDomainClass userClass = ga.getDomainClass(BidirectionalOneToManyAndCircularOneToManyUser.name)
        GrailsDomainClass uploadClass = ga.getDomainClass(BidirectionalOneToManyAndCircularOneToManyUpload.name)
        GrailsDomainClass uploadLogClass = ga.getDomainClass(BidirectionalOneToManyAndCircularOneToManyUploadLog.name)

        assertTrue userClass.getPropertyByName("users").isBidirectional()
        assertTrue userClass.getPropertyByName("users").isCircular()

        def uploadsProperty = userClass.getPropertyByName("uploads")

        assertTrue uploadsProperty.isBidirectional()
        assertTrue uploadsProperty.isOneToMany()

        def recipientProperty = uploadClass.getPropertyByName("recipient")

        assertTrue recipientProperty.isBidirectional()
        assertTrue recipientProperty.isManyToOne()

        assertEquals uploadsProperty.getOtherSide(), recipientProperty
        assertEquals recipientProperty.getOtherSide(), uploadsProperty

        def uploadLogsProperty = userClass.getPropertyByName("uploadLogs")

        assertTrue uploadLogsProperty.isBidirectional()
        assertTrue uploadLogsProperty.isOneToMany()

        def senderProperty = uploadLogClass.getPropertyByName("sender")

        assertTrue senderProperty.isBidirectional()
        assertTrue senderProperty.isManyToOne()

        assertEquals uploadLogsProperty.getOtherSide(), senderProperty
        assertEquals senderProperty.getOtherSide(), uploadLogsProperty

        def uploadSenderProperty = uploadClass.getPropertyByName("sender")

        assertTrue uploadSenderProperty.isOneToOne()
        assertFalse uploadSenderProperty.isBidirectional()
    }

    @Override
    protected getDomainClasses() {
        [BidirectionalOneToManyAndCircularOneToManyUser, BidirectionalOneToManyAndCircularOneToManyUpload, BidirectionalOneToManyAndCircularOneToManyUploadLog]
    }
}

class BidirectionalOneToManyAndCircularOneToManyUser implements java.io.Serializable {
    Long id
    Long version

    static hasMany = [users: BidirectionalOneToManyAndCircularOneToManyUser,
                      uploads: BidirectionalOneToManyAndCircularOneToManyUpload,
                      uploadLogs: BidirectionalOneToManyAndCircularOneToManyUploadLog]
    Set users
    Set uploads
    Set uploadLogs
    static mappedBy = [uploads: 'recipient', uploadLogs: 'sender']
    BidirectionalOneToManyAndCircularOneToManyUser manager
}

class BidirectionalOneToManyAndCircularOneToManyUploadLog implements Serializable {
    Long id
    Long version
    static belongsTo = BidirectionalOneToManyAndCircularOneToManyUser
    BidirectionalOneToManyAndCircularOneToManyUser sender
}

class BidirectionalOneToManyAndCircularOneToManyUpload implements Serializable {
    Long id
    Long version
    static belongsTo = [BidirectionalOneToManyAndCircularOneToManyUser]

    BidirectionalOneToManyAndCircularOneToManyUser sender
    BidirectionalOneToManyAndCircularOneToManyUser recipient
}
