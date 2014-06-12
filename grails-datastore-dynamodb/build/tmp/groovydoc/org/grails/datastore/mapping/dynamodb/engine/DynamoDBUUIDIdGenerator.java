package org.grails.datastore.mapping.dynamodb.engine;

import org.grails.datastore.mapping.model.PersistentEntity;

import java.util.UUID;

/**
 * Uses java UUID to generate a unique id.
 * @author Roman Stepanenko
 */
public class DynamoDBUUIDIdGenerator implements DynamoDBIdGenerator {
    public Object generateIdentifier(PersistentEntity persistentEntity, DynamoDBNativeItem nativeEntry) {
        return UUID.randomUUID().toString();
    }
}
