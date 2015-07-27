package org.grails.datastore.mapping.mongo.engine.codecs

import org.bson.codecs.configuration.CodecRegistry

/**
 * Can be implemented by codecs to be made aware of the registry
 *
 * @author Graeme Rocher
 * @since 4.1
 */
interface CodecRegistryAware {
    void setCodecRegistry(CodecRegistry codecRegistry)
}