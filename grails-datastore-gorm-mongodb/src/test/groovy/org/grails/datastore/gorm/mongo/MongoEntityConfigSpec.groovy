package org.grails.datastore.gorm.mongo

import com.mongodb.MongoClient
import grails.gorm.tests.GormDatastoreSpec

import org.grails.datastore.mapping.document.config.DocumentPersistentEntity
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.mongo.MongoSession
import org.grails.datastore.mapping.mongo.config.MongoAttribute
import org.grails.datastore.mapping.mongo.config.MongoCollection

import com.mongodb.DB
import com.mongodb.WriteConcern
import org.springframework.data.mongodb.core.MongoTemplate
import spock.lang.*

class MongoEntityConfigSpec extends GormDatastoreSpec{

    @IgnoreIf( { System.getenv('TRAVIS_BRANCH') != null } )
    def "Test custom collection config"() {
        given:
            session.mappingContext.addPersistentEntity MyMongoEntity

            def client = (MongoClient)session.nativeInterface
            DB db = client.getDB(session.defaultDatabase)

            db.dropDatabase()
            // db.resetIndexCache() // this method is missing from more recent driver versions

        when:
            PersistentEntity entity = session.mappingContext.getPersistentEntity(MyMongoEntity.name)

        then:
            entity instanceof DocumentPersistentEntity

        when:
            MongoCollection coll = entity.mapping.mappedForm
            MongoAttribute attr = entity.getPropertyByName("name").getMapping().getMappedForm()
            MongoAttribute location = entity.getPropertyByName("location").getMapping().getMappedForm()
        then:
            coll != null
            coll.collection == 'mycollection'
            coll.database == "test2"
            coll.writeConcern == WriteConcern.FSYNC_SAFE
            attr != null
            attr.index == true
            attr.targetName == 'myattribute'
            attr.indexAttributes == [unique:true]
            location != null
            location.index == true
            location.indexAttributes == [type:"2d"]
            coll.indices.size() == 1
            coll.indices[0].definition == [summary:"text"]

        when:
            MongoSession ms = session
        then:
            ms.getCollectionName(entity) == "mycollection"
    }
}

class MyMongoEntity {
    String id

    String name
    String location
    String summary

    static mapping = {
        collection "mycollection"
        database "test2"
        shard "name"
        writeConcern WriteConcern.FSYNC_SAFE
        index summary:"text"

        name index:true, attr:"myattribute", indexAttributes: [unique:true]

        location geoIndex:true
    }
}
