package org.grails.datastore.mapping.dynamodb.engine;

import org.grails.datastore.mapping.model.PersistentEntity;

/**
 * Encapsulates logic for generating id for a DynamoDB object.
 *
 * @author Roman Stepanenko
 */
public interface DynamoDBIdGenerator {
    Object generateIdentifier(final PersistentEntity persistentEntity, final DynamoDBNativeItem nativeEntry);
}
