package grails.gorm.rx.mongodb

import org.grails.datastore.mapping.mongo.config.MongoMappingContext
import org.grails.datastore.rx.mongodb.RxMongoDatastoreClient
import spock.lang.Shared
import spock.lang.Specification

abstract class RxGormSpec extends Specification {

    @Shared RxMongoDatastoreClient client

    void setupSpec() {
        def classes = getDomainClasses()
        client = new RxMongoDatastoreClient("test", classes as Class[])
    }

    void setup() {
        client.dropDatabase()
        client.rebuildIndex()
    }

    void cleanupSpec() {
        client?.close()
    }

    abstract List<Class> getDomainClasses()
}
