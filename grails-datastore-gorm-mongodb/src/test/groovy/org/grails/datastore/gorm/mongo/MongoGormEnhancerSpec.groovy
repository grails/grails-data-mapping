package org.grails.datastore.gorm.mongo

import grails.gorm.tests.GormDatastoreSpec

import com.mongodb.DBCollection

class MongoGormEnhancerSpec extends GormDatastoreSpec{

    def "Test getCollectionName static method" () {
        given:
            session.mappingContext.addPersistentEntity MyMongoEntity

        when:
            def collectionName = MyMongoEntity.collectionName

        then:
            collectionName == "mycollection"

    }

    def "Test getCollection static method" () {
        given:
            session.mappingContext.addPersistentEntity MyMongoEntity

        when:
            DBCollection collection = MyMongoEntity.collection

        then:
            collection.name == 'mycollection'
    }
}
