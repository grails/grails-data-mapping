package org.grails.gorm.rx.api

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.rx.RxDatastoreClient
import rx.Observable
import rx.Single

/**
 * Bridge to the implementation of the instance method level operations
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class RxGormInstanceApi<D> {

    final PersistentEntity entity
    final RxDatastoreClient datastoreClient

    RxGormInstanceApi(PersistentEntity entity, RxDatastoreClient datastoreClient) {
        this.entity = entity
        this.datastoreClient = datastoreClient
    }

    Observable<D> save(Object instance) {
        save(instance, Collections.<String,Object>emptyMap())
    }

    Observable<D> save(Object instance, Map<String, Object> arguments) {
        datastoreClient.persist(instance, arguments)
    }

}
