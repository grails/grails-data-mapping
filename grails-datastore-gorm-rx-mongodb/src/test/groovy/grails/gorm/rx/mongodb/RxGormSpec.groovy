package grails.gorm.rx.mongodb

import org.grails.datastore.mapping.mongo.config.MongoMappingContext
import org.grails.datastore.rx.mongodb.RxMongoDatastoreClient
import spock.lang.Specification

/**
 * Created by graemerocher on 06/05/16.
 */
abstract class RxGormSpec extends Specification {

    RxMongoDatastoreClient client

    void setup() {
        def context = new MongoMappingContext("test")

        def classes = getDomainClasses()
        context.addPersistentEntities(classes as Class[])
        context.initialize()
        client = new RxMongoDatastoreClient(context)
        client.mongoClient.getDatabase(client.defaultDatabase).drop().toBlocking().first()

    }

    void cleanup() {
        client?.close()
    }

    abstract List<Class> getDomainClasses()
}
