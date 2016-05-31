package org.grails.datastore.mapping.mongo.engine.codecs

import org.bson.BsonWriter
import org.bson.codecs.EncoderContext
import org.bson.codecs.configuration.CodecRegistry
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.model.PersistentProperty

/**
 * An interface for encoding PersistentProperty instances
 */
interface PropertyEncoder<T extends PersistentProperty> {
    void encode(BsonWriter writer, T property, Object value, EntityAccess parentAccess, EncoderContext encoderContext, CodecRegistry codecRegistry)
}
