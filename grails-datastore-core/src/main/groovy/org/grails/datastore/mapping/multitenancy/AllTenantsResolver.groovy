package org.grails.datastore.mapping.multitenancy

/**
 * For discriminator based multi-tenancy the tenant resolver has to be able to resolve all tenant ids in order to be able to iterate of the the available tenants
 *
 * Users can provide an implementation to discover the tenant ids here
 *
 * @author Graeme Rocher
 * @since 6.0
 */
interface AllTenantsResolver extends TenantResolver {

    /**
     * @return Resolves all tenant ids
     */
    Iterable<Serializable> resolveTenantIds();
}