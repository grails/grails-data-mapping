package org.grails.datastore.mapping.mongo

import grails.gorm.dirty.checking.DirtyCheck
import org.junit.Test

class LongIdTests extends AbstractMongoTest {

    @Test
    void testBasicPersistenceOperations() {
        md.mappingContext.addPersistentEntity(MongoLongIdEntity)

        AbstractMongoSession session = md.connect()

        session.nativeInterface.dropDatabase(session.defaultDatabase)

        def te = new MongoLongIdEntity(name:"Bob")

        session.persist te
        session.flush()

        assert te != null
        assert te.id != null
        assert te.id instanceof Long
        assert te.id == 1

        long previousId = te.id

        session.clear()
        te = session.retrieve(MongoLongIdEntity, te.id)

        assert te != null
        assert te.name == "Bob"

        te.name = "Fred"
        session.persist(te)
        session.flush()
        session.clear()

        te = session.retrieve(MongoLongIdEntity, te.id)
        assert te != null
        assert te.id != null
        assert te.name == 'Fred'

        session.delete te
        session.flush()

        te = session.retrieve(MongoLongIdEntity, te.id)
        assert te == null

        // check increment
        te = new MongoLongIdEntity(name:'Bob 2')
        session.persist te
        session.flush()

        assert te.id == 2
    }
}

@DirtyCheck
class MongoLongIdEntity {
    Long id
    String name
}
