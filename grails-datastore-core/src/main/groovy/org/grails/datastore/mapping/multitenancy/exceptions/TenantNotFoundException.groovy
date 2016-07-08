package org.grails.datastore.mapping.multitenancy.exceptions

import org.grails.datastore.mapping.core.DatastoreException

/**
 * Thrown when the tenant cannot be found
 *
 * @author Graeme Rocher
 * @since 6.0
 */
class TenantNotFoundException extends DatastoreException {
    TenantNotFoundException(String s) {
        super(s)
    }

    TenantNotFoundException(Class type) {
        super("No tenantId found for persistent class [$type.name]".toString())
    }

    TenantNotFoundException(String s, Throwable throwable) {
        super(s, throwable)
    }
}
