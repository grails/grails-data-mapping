package org.grails.datastore.gorm.mongo

import grails.gorm.tests.GormDatastoreSpec

import org.bson.types.ObjectId

class ObjectIdPersistenceSpec extends GormDatastoreSpec {

    def "Test that we can persist an object that has a BSON ObjectId"() {
        given:
            session.mappingContext.addPersistentEntity MongoObjectIdEntity

        when:
            def t = new MongoObjectIdEntity(name:"Bob").save(flush:true)
            session.clear()
            t = MongoObjectIdEntity.get(t.id)

        then:
            t != null
            t.id != null
    }
}

class MongoObjectIdEntity {
    ObjectId id

    String name
}
