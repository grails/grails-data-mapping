package org.grails.datastore.mapping.multitenancy.exceptions

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import org.grails.datastore.mapping.core.DatastoreException

/**
 * Exception thrown when an error occurs resolving the tenant
 *
 * @author Graeme Rocher
 * @since 6.0.4
 */
@CompileStatic
class TenantException extends DatastoreException {
    TenantException(String s) {
        super(s)
    }

    TenantException(String s, Throwable throwable) {
        super(s, throwable)
    }
}
