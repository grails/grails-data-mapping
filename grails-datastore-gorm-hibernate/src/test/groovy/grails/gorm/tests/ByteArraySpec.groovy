package grails.gorm.tests

import grails.persistence.Entity

/**
 * Created by graemerocher on 01/04/16.
 */
class ByteArraySpec extends GormDatastoreSpec {

    void "Test persist and query byte[] with dynamic finder"() {
        when:"a byte array is persisted"
        byte[] bytes = "foo".bytes

        new TestFile(name: "foo", bytes:bytes).save(flush:true)
        session.clear()

        then:"The value can be persisted with a dynamic finder"
        TestFile.findByBytes(bytes)
        TestFile.findByBytes(bytes)
    }
    @Override
    List getDomainClasses() {
        [TestFile]
    }
}

@Entity
class TestFile {
    String name
    byte[] bytes
}
