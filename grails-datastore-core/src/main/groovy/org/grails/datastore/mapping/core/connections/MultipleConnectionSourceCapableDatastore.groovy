package org.grails.datastore.mapping.core.connections

import org.grails.datastore.mapping.core.Datastore

/**
 * A {@link Datastore} capable of configuring multiple {@link Datastore} with individually named {@link ConnectionSource} instances
 *
 * @author Graeme Rocher
 * @since 6.1
 */
interface MultipleConnectionSourceCapableDatastore extends Datastore {

    /**
     * Lookup a {@link Datastore} by {@link ConnectionSource} name
     *
     * @param connectionName The connection name
     * @return The {@link Datastore}
     */
    Datastore getDatastoreForConnection(String connectionName)
}