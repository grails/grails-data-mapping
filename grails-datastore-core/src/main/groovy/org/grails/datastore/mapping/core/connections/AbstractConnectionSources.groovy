package org.grails.datastore.mapping.core.connections

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.core.DatastoreUtils
import org.springframework.core.env.PropertyResolver

/**
 * Abstract implementation of the {@link ConnectionSources} interface
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
abstract class AbstractConnectionSources <T, S extends ConnectionSourceSettings> implements ConnectionSources<T, S> {

    protected final ConnectionSource<T, S> defaultConnectionSource;
    protected final ConnectionSourceFactory<T, S> connectionSourceFactory;
    protected final PropertyResolver configuration;

    AbstractConnectionSources(ConnectionSource<T, S> defaultConnectionSource, ConnectionSourceFactory<T, S> connectionSourceFactory, PropertyResolver configuration) {
        if(connectionSourceFactory == null) {
            throw new IllegalArgumentException("Argument [connectionSourceFactory] cannot be null");
        }
        if(defaultConnectionSource == null) {
            throw new IllegalStateException("The default ConnectionSource cannot be null!");
        }
        if(configuration == null) {
            this.configuration = DatastoreUtils.createPropertyResolver(Collections.emptyMap());
        }
        else {
            this.configuration = configuration
        }
        this.defaultConnectionSource = defaultConnectionSource
        this.connectionSourceFactory = connectionSourceFactory
    }

    @Override
    PropertyResolver getBaseConfiguration() {
        return this.configuration
    }

    /**
     * Implementors should provide a method to lookup the data source names, which could be read from a database, configuration etc.
     *
     * @param connectionSourceFactory The factory class for construction {@link ConnectionSource} instances
     * @param configuration The root configuration
     * @return An iterable of connection source names. Should never return null.
     */
    protected abstract Iterable<String> getConnectionSourceNames(ConnectionSourceFactory<T, S> connectionSourceFactory, PropertyResolver configuration);

    @Override
    ConnectionSourceFactory<T, S> getFactory() {
        return this.connectionSourceFactory;
    }

    @Override
    ConnectionSource<T, S> getDefaultConnectionSource() {
        return this.defaultConnectionSource;
    }

    @Override
    public void close() throws IOException {
        for(ConnectionSource connectionSource : allConnectionSources) {
            connectionSource.close()
        }
    }

    @Override
    Iterator<ConnectionSource<T, S>> iterator() {
        allConnectionSources.iterator()
    }
}
