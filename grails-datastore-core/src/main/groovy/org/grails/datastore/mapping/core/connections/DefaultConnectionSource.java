package org.grails.datastore.mapping.core.connections;

import org.springframework.core.env.PropertyResolver;

/**
 * Abstract implementation of the {@link ConnectionSource} interface
 *
 * @author Graeme Rocher
 * @since 6.0
 */
public class DefaultConnectionSource<T, S extends ConnectionSourceSettings> implements ConnectionSource<T, S> {

    protected final String name;
    protected final T source;
    protected final S settings;

    public DefaultConnectionSource(String name, T source, S settings) {
        this.name = name;
        this.source = source;
        this.settings = settings;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public T getSource() {
        return this.source;
    }

    @Override
    public S getSettings() {
        return this.settings;
    }
}
