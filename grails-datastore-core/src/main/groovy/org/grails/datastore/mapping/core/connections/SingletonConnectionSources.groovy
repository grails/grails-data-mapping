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

    @Override
    void close() throws IOException {

        def source = defaultConnectionSource.source
        if(source instanceof Closeable) {
            ((Closeable)source).close()
        }
    }

    @Override
    Iterator<ConnectionSource<T, S>> iterator() {
        return allConnectionSources.iterator()
    }

    static class NullConnectionFactory<T, S extends ConnectionSourceSettings> implements ConnectionSourceFactory<T,S> {

        @Override
        ConnectionSource<T, S> create(String name, PropertyResolver configuration) {
            throw new UnsupportedOperationException("Cannot add a connection source it a SingletonConnectionSources")
        }

        @Override
        ConnectionSource<T, S> create(String name, PropertyResolver configuration, S fallbackSettings) {
            throw new UnsupportedOperationException("Cannot add a connection source it a SingletonConnectionSources")
        }

        @Override
        Serializable getConnectionSourcesConfigurationKey() {
            throw new UnsupportedOperationException("Cannot add a connection source it a SingletonConnectionSources")
        }
    }
}
