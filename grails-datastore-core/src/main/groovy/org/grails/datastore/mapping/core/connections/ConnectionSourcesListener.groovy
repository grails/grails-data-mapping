package org.grails.datastore.mapping.core.connections

/**
 * A listener for connection sources
 *
 * @author Graeme Rocher
 * @since 6.0
 */
interface ConnectionSourcesListener<T, S extends ConnectionSourceSettings> {

    /**
     *
     * Triggered when a new connection source is added at runtime
     *
     * @param connectionSource The connection source
     */
    void newConnectionSource(ConnectionSource<T,S> connectionSource)
}