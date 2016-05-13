package org.grails.datastore.rx.mongodb.client

import com.mongodb.rx.client.MongoClient
import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import org.bson.codecs.configuration.CodecRegistry
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.mongo.config.MongoMappingContext
import org.grails.datastore.mapping.query.Query
import org.grails.datastore.rx.mongodb.RxMongoDatastoreClient
import org.grails.datastore.rx.mongodb.query.RxMongoQuery
import org.grails.datastore.rx.query.QueryState

/**
 * overrides the default client and provides the ability to customize the target database name, collection name or client connection
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@InheritConstructors
@CompileStatic
class DelegatingRxMongoDatastoreClient extends RxMongoDatastoreClient {

    final @Delegate RxMongoDatastoreClient datastoreClient

    String targetCollectionName
    String targetDatabaseName
    MongoClient targetMongoClient

    DelegatingRxMongoDatastoreClient(RxMongoDatastoreClient datastoreClient) {
        super(datastoreClient.nativeInterface, datastoreClient.mappingContext)
        this.datastoreClient = datastoreClient
    }

    @Override
    protected void initialize(MongoMappingContext mappingContext) {
        // no-op, use state of delegate
    }

    @Override
    protected CodecRegistry createCodeRegistry() {
        return null
    }

    @Override
    CodecRegistry getCodecRegistry() {
        return datastoreClient.getCodecRegistry()
    }

    @Override
    MongoClient getNativeInterface() {
        if(targetMongoClient != null) {
            return targetMongoClient
        }
        else {
            return datastoreClient.getNativeInterface()
        }
    }

    @Override
    String getCollectionName(PersistentEntity entity) {
        if(targetCollectionName != null) {
            return targetCollectionName
        }
        else {
            return datastoreClient.getCollectionName(entity)
        }
    }

    @Override
    String getDatabaseName(PersistentEntity entity) {
        if(targetDatabaseName != null) {
            return targetDatabaseName
        }
        else {
            return datastoreClient.getDatabaseName(entity)
        }
    }

    @Override
    Query createEntityQuery(PersistentEntity entity, QueryState queryState) {
        return new RxMongoQuery(this, entity, queryState)
    }
}
