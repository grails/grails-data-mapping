package org.grails.datastore.mapping.mongo

import grails.gorm.dirty.checking.DirtyCheck
import org.bson.types.ObjectId
import org.junit.Test

class ObjectIdTests extends AbstractMongoTest {

    @Test
    void testBasicPersistenceOperations() {
        md.mappingContext.addPersistentEntity(MongoObjectIdEntity)

        AbstractMongoSession session = md.connect()

        session.nativeInterface.dropDatabase(session.defaultDatabase)

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

@DirtyCheck
class MongoObjectIdEntity {
    ObjectId id
    String name
}
