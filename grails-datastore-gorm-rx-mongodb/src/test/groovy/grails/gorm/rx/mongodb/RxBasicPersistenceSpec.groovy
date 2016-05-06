package grails.gorm.rx.mongodb

import grails.gorm.annotation.Entity
import org.grails.datastore.gorm.GormEntity
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import org.grails.datastore.mapping.mongo.config.MongoMappingContext
import org.grails.datastore.rx.mongodb.RxMongoDatastoreClient
import rx.Observable
import spock.lang.Specification

/**
 * Created by graemerocher on 04/05/16.
 */
class RxBasicPersistenceSpec extends Specification {

    void "Test basic persistence of MongoDB using RX"() {
        given:"An rx datastore client"

        def context = new MongoMappingContext("test")
        context.addPersistentEntity(Simple)
        context.initialize()
        def client = new RxMongoDatastoreClient(context)
        client.mongoClient.getDatabase(client.defaultDatabase).drop().toBlocking().first()
//
//        when:"An object is queried that doesn't exist"
//        def nonExistant = Simple.get(new ObjectId())
//
//        then:"The object doesn't exist"
//        nonExistant.toBlocking().first() == null

        when:"An instance is saved"
        def s = new Simple(name: "Fred")
        s = s.save().toBlocking().first()

        then:"An id is assigned"
        s.id != null

        when:"An object is queried"
        Observable<Simple> o = Simple.get(s.id)
        def result = o.toBlocking().single()

        then:"It is returned correctly"
        result.name == 'Fred'

        when:"The object is updated"
        result.name = "Bob"
        result.save().toBlocking().single()
        result = Simple.get(result.id)
        result = result.toBlocking().single()

        then:"The result is correct"
        result != null
        result.name == 'Bob'

        when:"All results are listed"
        new Simple(name:"Joe").save().toBlocking().single()
        def results = Simple.list()

        then:"The results are correct"
        results.toBlocking().iterator.toList().size() == 2
    }

    void "Test entity structure"() {
        expect:
        RxMongoEntity.isAssignableFrom(Simple)
        !GormEntity.isAssignableFrom(Simple)
        DirtyCheckable.isAssignableFrom(Simple)
    }
}

