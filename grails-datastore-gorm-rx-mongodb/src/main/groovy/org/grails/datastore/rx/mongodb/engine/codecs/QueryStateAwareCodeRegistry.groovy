package org.grails.datastore.rx.mongodb.engine.codecs

import groovy.transform.CompileStatic
import org.bson.codecs.Codec
import org.bson.codecs.configuration.CodecRegistry
import org.grails.datastore.rx.mongodb.RxMongoDatastoreClient
import org.grails.datastore.rx.query.QueryState
/**
 * A {@link CodecRegistry} that maintains a query state of loaded entities to avoid repeated loading of entities in association graphs
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class QueryStateAwareCodeRegistry implements CodecRegistry {

    final CodecRegistry parent
    final QueryState queryState
    final RxMongoDatastoreClient datastoreClient

    QueryStateAwareCodeRegistry(CodecRegistry parent, QueryState queryState, RxMongoDatastoreClient datastoreClient) {
        this.parent = parent
        this.queryState = queryState
        this.datastoreClient = datastoreClient
    }

    @Override
    def <T> Codec<T> get(Class<T> aClass) {

        def entity = datastoreClient.getMappingContext().getPersistentEntity(aClass.name)
        if(entity != null) {
            return new RxPersistentEntityCodec(entity, datastoreClient, queryState)
        }
        else {
            return parent.get(aClass)
        }
    }

}
