package org.grails.datastore.mapping.core.connections

import groovy.transform.CompileStatic
import org.springframework.core.env.PropertyResolver

/**
 * Models a {@link ConnectionSources} object that only supports a single connection
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class SingletonConnectionSources<T, S extends ConnectionSourceSettings> extends AbstractConnectionSources<T, S> {

    SingletonConnectionSources(ConnectionSource<T,S> connectionSource, PropertyResolver configuration) {
        super(connectionSource, new NullConnectionFactory(), configuration)
    }
    @Override
    protected Iterable<String> getConnectionSourceNames(ConnectionSourceFactory<T, S> connectionSourceFactory, PropertyResolver configuration) {
        Arrays.asList(ConnectionSource.DEFAULT)
    }

    @Override
    Iterable<ConnectionSource<T, S>> getAllConnectionSources() {
        return Arrays.asList(defaultConnectionSource)
    }

    @Override
    ConnectionSource<T, S> getConnectionSource(String name) {
        return defaultConnectionSource
    }

    @Override
    ConnectionSource<T, S> addConnectionSource(String name, PropertyResolver configuration) {
        throw new UnsupportedOperationException("Cannot add a connection source it a SingletonConnectionSources")
    }

    static class NullConnectionFactory<T, S extends ConnectionSourceSettings> extends AbstractConnectionSourceFactory<T,S> {

        @Override
        protected <F extends ConnectionSourceSettings> S buildSettings(String name, PropertyResolver configuration, F fallbackSettings, boolean isDefaultDataSource) {
            throw new UnsupportedOperationException("Cannot add a connection source it a SingletonConnectionSources")
        }

        @Override
        ConnectionSource<T, S> create(String name, S settings) {
            throw new UnsupportedOperationException("Cannot add a connection source it a SingletonConnectionSources")
        }

        @Override
        Serializable getConnectionSourcesConfigurationKey() {
            throw new UnsupportedOperationException("Cannot add a connection source it a SingletonConnectionSources")
        }
    }
}
