package org.grails.datastore.mapping.multitenancy;

import org.grails.datastore.mapping.core.connections.ConnectionSourceSettings;

/**
 * For datastores that are capable of implementing the addition of new schemas at runtime for a single shared database instance
 *
 * @author Graeme Rocher
 * @since 6.1
 */
public interface SchemaMultiTenantCapableDatastore<T, S extends ConnectionSourceSettings> extends MultiTenantCapableDatastore<T, S> {

    /**
     * Add a new tenant at runtime for the given schema name
     *
     * @param schemaName The schema name
     */
    void addTenantForSchema(String schemaName);
}
