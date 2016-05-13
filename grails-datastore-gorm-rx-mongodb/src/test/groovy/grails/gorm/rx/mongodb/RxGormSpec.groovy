package grails.gorm.rx.mongodb

import org.grails.datastore.mapping.mongo.config.MongoMappingContext
import org.grails.datastore.rx.mongodb.RxMongoDatastoreClient
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created by graemerocher on 06/05/16.
 */
abstract class RxGormSpec extends Specification {

    @Shared RxMongoDatastoreClient client

    void setupSpec() {
        def context = new MongoMappingContext("test")

        def classes = getDomainClasses()
        context.addPersistentEntities(classes as Class[])
        for(c in classes) {
            GroovySystem.metaClassRegistry.removeMetaClass(c)
        }
        context.initialize()
        client = new RxMongoDatastoreClient(context)


    }

    void setup() {
        client.nativeInterface.getDatabase(client.defaultDatabase).drop().toBlocking().first()
        client.rebuildIndex()
    }

    void cleanupSpec() {
        client?.close()
    }

    abstract List<Class> getDomainClasses()
}
