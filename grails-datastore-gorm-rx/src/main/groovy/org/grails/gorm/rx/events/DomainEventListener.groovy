package org.grails.gorm.rx.events

import groovy.transform.CompileStatic
import org.grails.datastore.rx.RxDatastoreClient

/**
 * An domain event listener for RxGORM
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class DomainEventListener extends org.grails.datastore.gorm.events.DomainEventListener {

    final RxDatastoreClient datastoreClient

    DomainEventListener(RxDatastoreClient datastoreClient) {
        super(datastoreClient.mappingContext)
        this.datastoreClient = datastoreClient
    }

    @Override
    boolean supportsSourceType(Class<?> sourceType) {
        datastoreClient.getClass().equals(sourceType)
    }

}
