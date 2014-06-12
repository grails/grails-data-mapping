package org.grails.datastore.mapping.mongo

import org.bson.types.ObjectId
import org.junit.Test

class ObjectIdTests extends AbstractMongoTest {

    @Test
    void testBasicPersistenceOperations() {
        md.mappingContext.addPersistentEntity(MongoObjectIdEntity)

        MongoSession session = md.connect()

        session.nativeInterface.dropDatabase()

        def te = new MongoObjectIdEntity(name:"Bob")

        session.persist te
        session.flush()

        assert te != null
        assert te.id != null
        assert te.id instanceof ObjectId

        session.clear()
        te = session.retrieve(MongoObjectIdEntity, te.id)

        assert te != null
        assert te.name == "Bob"

        te.name = "Fred"
        session.persist(te)
        session.flush()
        session.clear()

        te = session.retrieve(MongoObjectIdEntity, te.id)
        assert te != null
        assert te.id != null
        assert te.name == 'Fred'

        session.delete te
        session.flush()

        te = session.retrieve(MongoObjectIdEntity, te.id)
        assert te == null
    }
}

class MongoObjectIdEntity {
    ObjectId id
    String name
}
