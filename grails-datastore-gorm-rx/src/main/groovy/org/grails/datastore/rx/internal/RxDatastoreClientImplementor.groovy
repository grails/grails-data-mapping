package org.grails.datastore.rx.internal

import grails.gorm.rx.proxy.ObservableProxy
import org.grails.datastore.mapping.query.Query
import org.grails.datastore.rx.query.QueryState
import rx.Observable

/**
 * Internal non-client methods implemented by the implementor
 *
 * @author Graeme Rocher
 * @since 6.0
 */
interface RxDatastoreClientImplementor {

    /**
     * Obtain an instance passing the query state
     *
     * @param type The type
     * @param id The id
     * @param queryState The query state
     * @return The observable
     */
    public <T> Observable<T> get(Class<T> type, Serializable id, QueryState queryState)

    /**
     * Obtain an {@link grails.gorm.rx.proxy.ObservableProxy} for the given type and id
     *
     * @param type The type
     * @param id The id
     * @return An {@link grails.gorm.rx.proxy.ObservableProxy}
     */
    def <T1> ObservableProxy<T1> proxy(Class<T1> type, Serializable id, QueryState queryState)

    /**
     * Obtain an {@link ObservableProxy} that executes the given query to initialize
     *
     * @param query The query
     * @return An {@link ObservableProxy}
     */
    def ObservableProxy proxy(Query query, QueryState queryState)

    /**
     * Create a query, passing the entity state
     *
     * @param type The type
     * @param queryState The query state
     * @return The query
     */
    Query createQuery(Class type, QueryState queryState)

}