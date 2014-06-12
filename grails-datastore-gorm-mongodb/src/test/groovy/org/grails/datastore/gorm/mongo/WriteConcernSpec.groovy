package org.grails.datastore.gorm.mongo

import grails.gorm.tests.GormDatastoreSpec
import grails.persistence.Entity

import com.mongodb.WriteConcern

/**
 * Tests usage of WriteConcern
 */
class WriteConcernSpec extends GormDatastoreSpec {

    @Override
    List getDomainClasses() {
        [SafeWrite]
    }

    void "Test that the correct WriteConcern is used to save entities"() {
        when:"An object is saved"
            def sw = new SafeWrite(name:"Bob")
            sw.save(flush:true)

        then:"The correct write concern is used"
            sw != null
    }
}

@Entity
class SafeWrite {
    String id
    String name
    static mapping = {
        writeConcern WriteConcern.FSYNC_SAFE
    }
}
