package org.grails.datastore.mapping.config

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import org.springframework.core.env.StandardEnvironment

/**
 * An environment for GORM
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
@InheritConstructors
class DatastoreEnvironment extends StandardEnvironment {
}
