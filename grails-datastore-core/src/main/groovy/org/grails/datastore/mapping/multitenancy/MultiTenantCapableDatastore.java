package org.grails.datastore.mapping.multitenancy;

import groovy.lang.Closure;
import org.grails.datastore.mapping.core.Datastore;
import org.grails.datastore.mapping.core.connections.ConnectionSourceSettings;
import org.grails.datastore.mapping.core.connections.ConnectionSourcesProvider;

import java.io.Serializable;

/**
 * An implementation that is capable of multi tenancy
 *
 * @author Graeme Rocher
 * @since 6.0
 */
public interface MultiTenantCapableDatastore<T, S extends ConnectionSourceSettings> extends Datastore, ConnectionSourcesProvider<T, S> {

    /**
     * @return The multi tenancy mode
     */
    MultiTenancySettings.MultiTenancyMode getMultiTenancyMode();

    /**
     * @return Obtain the tenant resolver
     */
    TenantResolver getTenantResolver();

    /**
     * Obtains the datastore for the given tenant id. In SINGLE mode
     * this will be a unique datastore for each tenant. For MULTI mode
     * a single datastore is used for all tenants
     *
     * @param tenantId The tenant id
     * @return The datastore
     */
    Datastore getDatastoreForTenantId(Serializable tenantId);


    /**
     * Execute a new session with the given tenantId
     *
     * @param tenantId The tenant id
     * @param callable the callable
     * @param <T1> the return type
     * @return The return value of the closure
     */
    <T1> T1 withNewSession(Serializable tenantId, Closure<T1> callable);
}
