package org.grails.gorm.rx.api

import org.grails.datastore.gorm.finders.DynamicFinder
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.query.Query
import org.grails.datastore.rx.RxDatastoreClient
import org.grails.datastore.rx.query.RxQuery
import rx.Observable

/**
 * Bridge to the implementation of the static method level operations for RX GORM
 *
 * @author Graeme Rocher
 * @since 6.0
 */
class RxGormStaticApi<D> {

    final PersistentEntity entity
    final RxDatastoreClient datastoreClient

    RxGormStaticApi(PersistentEntity entity, RxDatastoreClient datastoreClient) {
        this.entity = entity
        this.datastoreClient = datastoreClient
    }

    Observable<D> get(Serializable id) {
        datastoreClient.get(entity.javaClass, id)
    }

    Observable<D> list(Map params = Collections.emptyMap()) {
        def query = datastoreClient.createQuery(entity.javaClass)
        DynamicFinder.populateArgumentsForCriteria(entity.javaClass, query, params)
        return ((RxQuery<D>) query).findAll()
    }

    Observable<D> methodMissing(String methodName, arg) {

    }
}
