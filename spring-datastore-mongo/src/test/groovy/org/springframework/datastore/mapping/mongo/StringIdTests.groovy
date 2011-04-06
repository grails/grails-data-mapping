package org.springframework.datastore.mapping.mongo

import org.junit.Test

class StringIdTests extends AbstractMongoTest {

    @Test
    void testBasicPersistenceOperations() {
        md.mappingContext.addPersistentEntity(MongoStringIdEntity)

        MongoSession session = md.connect()

        session.nativeInterface.dropDatabase()

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

class MongoStringIdEntity {
    String id
    String name
}
