package org.grails.datastore.mapping.mongo

import grails.gorm.dirty.checking.DirtyCheck
import org.junit.Test

class StringIdTests extends AbstractMongoTest {

    @Test
    void testBasicPersistenceOperations() {
        md.mappingContext.addPersistentEntity(MongoStringIdEntity)

        AbstractMongoSession session = md.connect()

        session.nativeInterface.dropDatabase(session.defaultDatabase)

        def te = new MongoStringIdEntity(name:"Bob")

        session.persist te
        session.flush()

        assert te != null
        assert te.id != null
        assert te.id instanceof String

        session.clear()
        te = session.retrieve(MongoStringIdEntity, te.id)

        assert te != null
        assert te.name == "Bob"

        te.name = "Fred"
        session.persist(te)
        session.flush()
        session.clear()

        te = session.retrieve(MongoStringIdEntity, te.id)
        assert te != null
        assert te.id != null
        assert te.name == 'Fred'

        session.delete te
        session.flush()

        te = session.retrieve(MongoStringIdEntity, te.id)
        assert te == null
    }
}

@DirtyCheck
class MongoStringIdEntity {
    String id
    String name
}
