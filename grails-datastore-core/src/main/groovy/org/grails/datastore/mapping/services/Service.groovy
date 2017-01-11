package org.grails.datastore.mapping.services

import org.grails.datastore.mapping.core.Datastore

/**
 * Represents a service available exposed by the GORM {@link Datastore}
 *
 * @author Graeme Rocher
 * @since 6.1
 */
trait Service {

    /**
     * The datastore that this service is related to
     */
    Datastore datastore
}