package org.grails.datastore.gorm.mongo

import com.mongodb.client.MongoCollection
import grails.gorm.tests.GormDatastoreSpec

import com.mongodb.DBCollection
import spock.lang.*

@IgnoreIf( { System.getenv('TRAVIS_BRANCH') != null } )
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
            MongoCollection collection = MyMongoEntity.collection

        then:
            collection.name == 'mycollection'
    }
}
