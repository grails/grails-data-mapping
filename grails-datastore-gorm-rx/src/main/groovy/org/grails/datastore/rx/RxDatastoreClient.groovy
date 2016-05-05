package org.grails.datastore.rx

import rx.Observable
import rx.Single

/**
 * Represents a client connection pool that can be used to interact with a backing implementation
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
    public <T> Observable<T> get(Class type, Serializable id)

    /**
     * Persist and instance and return the observable
     *
     * @param instance The instance
     * @param arguments The arguments
     * @return The observable
     */
    def <T> Observable<T> persist(Object instance, Map<String, Object> arguments)

    /**
     * Persist and instance and return the observable
     *
     * @param instance The instance
     * @param arguments The arguments
     * @return The observable
     */
    def <T> Observable<T> persist(Object instance)

    /**
     * @return The native interface to the datastore
     */
    T getNativeInterface()
}