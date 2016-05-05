package grails.gorm.rx.mongodb

import grails.gorm.rx.RxGormEntity
import org.bson.types.ObjectId
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

    }
}

class Simple implements RxMongoEntity<Simple> {
    String name
}
