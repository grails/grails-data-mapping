package org.grails.datastore.mapping.multitenancy.resolvers

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.multitenancy.TenantResolver
import org.grails.datastore.mapping.multitenancy.exceptions.TenantNotFoundException

/**
 * A {@link TenantResolver} that throws an exception indicating the tenant id was not found
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class NoTenantResolver implements TenantResolver {
    @Override
    Serializable resolveTenantIdentifier(Class persistentClass) {
        throw new TenantNotFoundException("No tenantId found for persistent class [$persistentClass.name]")
    }
}
