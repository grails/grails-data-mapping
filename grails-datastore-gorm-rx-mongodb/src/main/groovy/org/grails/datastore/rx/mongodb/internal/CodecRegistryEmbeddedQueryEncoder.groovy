package org.grails.datastore.rx.mongodb.internal

import groovy.transform.CompileStatic
import org.bson.BsonDocument
import org.bson.BsonDocumentWriter
import org.bson.codecs.configuration.CodecRegistry
import org.grails.datastore.mapping.model.types.Embedded
import org.grails.datastore.mapping.mongo.engine.codecs.PersistentEntityCodec
import org.grails.datastore.mapping.mongo.query.EmbeddedQueryEncoder
import org.grails.datastore.mapping.mongo.query.MongoQuery
import org.grails.datastore.rx.RxDatastoreClient
import org.grails.datastore.rx.mongodb.RxMongoDatastoreClient
import org.grails.datastore.rx.mongodb.engine.codecs.RxPersistentEntityCodec

/**
 * Used to encode embedded query items using a {@link CodecRegistry}
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class CodecRegistryEmbeddedQueryEncoder implements EmbeddedQueryEncoder {

    final RxMongoDatastoreClient datastoreClient

    CodecRegistryEmbeddedQueryEncoder(RxMongoDatastoreClient datastoreClient) {
        this.datastoreClient = datastoreClient
    }

    @Override
    Object encode(Embedded embedded, Object instance) {
        def codec = new RxPersistentEntityCodec(embedded.associatedEntity, datastoreClient)
        final BsonDocument doc = new BsonDocument();
        codec.encode(new BsonDocumentWriter(doc), instance, MongoQuery.ENCODER_CONTEXT, false);
        return doc;
    }
}
