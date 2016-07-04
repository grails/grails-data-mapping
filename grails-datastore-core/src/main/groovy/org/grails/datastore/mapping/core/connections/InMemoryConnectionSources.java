package org.grails.datastore.mapping.core.connections;

import org.springframework.core.env.PropertyResolver;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
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
    protected Iterable<String> getConnectionSourceNames(ConnectionSourceFactory<T, S> connectionSourceFactory, PropertyResolver configuration) {
        Map<String, Object> allConnectionSources = configuration.getProperty(connectionSourceFactory.getConnectionSourcesConfigurationKey().toString(), Map.class, Collections.emptyMap());
        return allConnectionSources.keySet();
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

        ConnectionSource<T, S> connectionSource = connectionSourceFactory.create(name, configuration, this.defaultConnectionSource.getSettings());
        if(connectionSource == null) {
            throw new IllegalStateException("ConnectionSource factory returned null");
        }
        this.connectionSourceMap.put(name, connectionSource);
        return connectionSource;
    }

    @Override
    public Iterator<ConnectionSource<T, S>> iterator() {
        return getAllConnectionSources().iterator();
    }

    @Override
    public void close() throws IOException {
        for(ConnectionSource connectionSource : this.connectionSourceMap.values()) {
            Object source = connectionSource.getSource();
            if(source instanceof Closeable) {
                ((Closeable)source).close();
            }
        }
    }
}
