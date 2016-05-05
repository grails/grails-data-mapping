package org.grails.datastore.rx

import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.query.Query
import rx.Observable
import rx.Single

/**
 * Represents a client connection pool that can be used to interact with a backing implementation in RxGORM
 *
 * @author Graeme Rocher
 * @since 6.0
 *
 * @param The native client interface
 */
interface RxDatastoreClient<T> extends Closeable {

    /**
     * Obtains a single instance for the given type and id
     *
     * @param type The persistent type
     * @param id The identifier
     *
     * @return A single observable result
     */
    public <T1> Observable<T1> get(Class<T1> type, Serializable id)

    /**
     * Persist and instance and return the observable
     *
     * @param instance The instance
     * @param arguments The arguments
     * @return The observable
     */
    def <T1> Observable<T1> persist(T1 instance, Map<String, Object> arguments)

    /**
     * Persist and instance and return the observable
     *
     * @param instance The instance
     * @param arguments The arguments
     * @return The observable
     */
    def <T1> Observable<T1> persist(T1 instance)

    /**
     * Creates a query for the given type
     *
     * @param type The type
     * @return The query
     */
    Query createQuery(Class type)

    /**
     * @return The native interface to the datastore
     */
    T getNativeInterface()

    /**
     * @return The mapping context
     */
    MappingContext getMappingContext()
}