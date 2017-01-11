package org.grails.datastore.mapping.services

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import org.grails.datastore.mapping.core.DatastoreException

/**
 * Thrown when a service cannot be found for the given type
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
class ServiceNotFoundException extends DatastoreException {
    ServiceNotFoundException(String s) {
        super(s)
    }
}
