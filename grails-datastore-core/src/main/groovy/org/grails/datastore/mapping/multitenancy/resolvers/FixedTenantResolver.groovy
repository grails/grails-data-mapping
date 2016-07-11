package org.grails.datastore.mapping.multitenancy.resolvers

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.core.connections.ConnectionSource
import org.grails.datastore.mapping.multitenancy.TenantResolver

/**
 * A tenant resolver that resolves to a fixed static named tenant id
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class FixedTenantResolver implements TenantResolver {
    /**
     * The tenant id to resolve to
     */
    final Serializable tenantId

    FixedTenantResolver() {
        tenantId = ConnectionSource.DEFAULT
    }

    FixedTenantResolver(Serializable tenantId) {
        if(tenantId == null) {
            throw new IllegalArgumentException("Argument [tenantId] cannot be null")
        }
        this.tenantId = tenantId
    }

    @Override
    Serializable resolveTenantIdentifier() {
        return tenantId
    }
}
