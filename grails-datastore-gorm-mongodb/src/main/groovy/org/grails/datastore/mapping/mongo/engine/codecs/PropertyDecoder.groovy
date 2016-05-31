package org.grails.datastore.mapping.mongo.engine.codecs

import org.bson.BsonReader
import org.bson.codecs.DecoderContext
import org.bson.codecs.configuration.CodecRegistry
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.model.PersistentProperty

/**
 * An interface for encoding PersistentProperty instances
 */
interface PropertyDecoder<T extends PersistentProperty> {
    void decode(BsonReader reader, T property, EntityAccess entityAccess, DecoderContext decoderContext, CodecRegistry codecRegistry)
}
