package org.grails.datastore.mapping.core.connections;

import org.springframework.core.env.PropertyResolver;

import java.io.Serializable;

/**
 * Abstract implementation of the {@link ConnectionSourceFactory} interface
 *
 * @author Graeme Rocher
 * @since 6.0
 */
public abstract class AbstractConnectionSourceFactory<T, S extends ConnectionSourceSettings> implements ConnectionSourceFactory<T, S> {
    @Override
    public ConnectionSource<T, S> create(String name, PropertyResolver configuration) {
        ConnectionSourceSettingsBuilder builder = new ConnectionSourceSettingsBuilder(configuration);
        ConnectionSourceSettings fallbackSettings = builder.build();

        return create(name, configuration, fallbackSettings);
    }

    @Override
    public ConnectionSource<T, S> create(String name, PropertyResolver configuration, ConnectionSource<T, S> fallbackConnectionSource) {
        return create(name, configuration, fallbackConnectionSource.getSettings());
    }

    @Override
    public final <F extends ConnectionSourceSettings> ConnectionSource<T, S> create(String name, PropertyResolver configuration, F fallbackSettings) {

        boolean isDefaultDataSource = ConnectionSource.DEFAULT.equals(name);
        S settings = buildSettings(name, configuration, fallbackSettings, isDefaultDataSource);
        return create(name, settings);
    }

    @Override
    public ConnectionSource<T, S> createRuntime(String name, PropertyResolver configuration, S fallbackSettings) {
        S settings = buildSettings(name, configuration, fallbackSettings, false);
        return create(name, settings);
    }

    protected abstract <F extends ConnectionSourceSettings> S buildSettings(String name, PropertyResolver configuration, F fallbackSettings, boolean isDefaultDataSource);
}
