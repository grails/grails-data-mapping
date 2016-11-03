package org.grails.datastore.mapping.multitenancy.exceptions

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.core.DatastoreException

/**
 * Thrown when the tenant cannot be found
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class TenantNotFoundException extends TenantException {
    TenantNotFoundException(String s) {
        super(s)
    }

    TenantNotFoundException() {
        super("No tenantId found")
    }

    TenantNotFoundException(String s, Throwable throwable) {
        super(s, throwable)
    }
}
