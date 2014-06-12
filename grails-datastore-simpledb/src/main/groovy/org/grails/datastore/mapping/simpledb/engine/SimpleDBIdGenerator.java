package org.grails.datastore.mapping.simpledb.engine;

import org.grails.datastore.mapping.model.PersistentEntity;

/**
 * Encapsulates logic for generating id for a SimpleDB object.
 *
 * @author Roman Stepanenko
 */
public interface SimpleDBIdGenerator {
    Object generateIdentifier(final PersistentEntity persistentEntity, final SimpleDBNativeItem nativeEntry);
}
