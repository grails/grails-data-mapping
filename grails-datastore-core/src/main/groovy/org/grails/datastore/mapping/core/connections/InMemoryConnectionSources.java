package org.grails.datastore.mapping.core.connections;

import org.springframework.core.env.PropertyResolver;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of the {@link ConnectionSources} interface. This implementation reads {@link ConnectionSource} implementations from configuration and stores them in-memory
 *
 * @author Graeme Rocher
 * @since 6.0
 */
public class InMemoryConnectionSources<T, S extends ConnectionSourceSettings> extends AbstractConnectionSources<T, S> {

    protected final Map<String, ConnectionSource<T, S>> connectionSourceMap = new ConcurrentHashMap<>();

    public InMemoryConnectionSources(ConnectionSource<T, S> defaultConnectionSource, ConnectionSourceFactory<T, S> connectionSourceFactory, PropertyResolver configuration) {
        super(defaultConnectionSource, connectionSourceFactory, configuration);
        this.connectionSourceMap.put(ConnectionSource.DEFAULT, defaultConnectionSource);

        for(String name : getConnectionSourceNames(connectionSourceFactory, configuration)) {
            ConnectionSource<T, S> connectionSource = connectionSourceFactory.create(name, configuration, defaultConnectionSource.getSettings());
            if(connectionSource != null) {
                this.connectionSourceMap.put(name, connectionSource);
            }
        }
    }

    @Override
    public Iterable<ConnectionSource<T, S>> getAllConnectionSources() {
        return Collections.unmodifiableCollection(this.connectionSourceMap.values());
    }

    @Override
    public ConnectionSource<T, S> getConnectionSource(String name) {
        return this.connectionSourceMap.get(name);
    }

    @Override
    public ConnectionSource<T, S> addConnectionSource(String name, PropertyResolver configuration) {
        if(name == null) {
            throw new IllegalArgumentException("Argument [name] cannot be null");
        }
        if(configuration == null) {
            throw new IllegalArgumentException("Argument [configuration] cannot be null");
        }

        ConnectionSource<T, S> connectionSource = connectionSourceFactory.createRuntime(name, configuration, this.defaultConnectionSource.getSettings());
        if(connectionSource == null) {
            throw new IllegalStateException("ConnectionSource factory returned null");
        }
        this.connectionSourceMap.put(name, connectionSource);
        return connectionSource;
    }


}
