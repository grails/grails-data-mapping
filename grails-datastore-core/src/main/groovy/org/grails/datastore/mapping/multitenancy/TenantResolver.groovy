package org.grails.datastore.mapping.multitenancy

import org.grails.datastore.mapping.multitenancy.exceptions.TenantNotFoundException

/**
 * An interface for applications that implement Multi Tenancy to implement in order to resolve the current identifier
 *
 * @author Graeme Rocher
 * @since 6.0
 */
interface TenantResolver {

    /**
     * <p>Resolves the current tenant identifier. In a Single tenancy setup where each tenant has their own database this would be
     * the name of the {@link org.grails.datastore.mapping.core.connections.ConnectionSource}.</p>
     *
     * <p>In a Multi Tenant setup where a single database is being used amongst multiple tenants this would be the object that is used
     * as the tenantId property for each domain class.</p>
     *
     * @param persistentClass The class that the resolver is resolving the id for
     *
     * @return The tenant identifier
     *
     */
    public Serializable resolveTenantIdentifier(Class persistentClass) throws TenantNotFoundException
}