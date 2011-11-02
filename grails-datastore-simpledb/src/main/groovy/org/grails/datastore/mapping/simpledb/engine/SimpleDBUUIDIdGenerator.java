package org.grails.datastore.mapping.simpledb.engine;

import org.grails.datastore.mapping.model.PersistentEntity;

import java.util.UUID;

/**
 * Uses java UUID to generate a unique id.
 * @author Roman Stepanenko
 */
public class SimpleDBUUIDIdGenerator implements SimpleDBIdGenerator {
    public Object generateIdentifier(PersistentEntity persistentEntity, SimpleDBNativeItem nativeEntry) {
        return UUID.randomUUID().toString();
    }
}
