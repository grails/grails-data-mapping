package org.grails.datastore.mapping.core.connections

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.core.exceptions.ConfigurationException
import org.springframework.core.env.PropertyResolver

/**
 * Initializer sequence for creating {@link ConnectionSources}
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class ConnectionSourcesInitializer {

    /**
     * Creates the {@link ConnectionSources} for the given factory and configuration
     *
     * @param connectionSourceFactory The factory
     * @param configuration The configuration
     * @return The {@link ConnectionSources}
     */
    public static <T,S extends ConnectionSourceSettings> ConnectionSources create(ConnectionSourceFactory<T, S> connectionSourceFactory, PropertyResolver configuration) {
        ConnectionSource defaultConnectionSource = connectionSourceFactory.create(ConnectionSource.DEFAULT, configuration);
        Class<ConnectionSources> connectionSourcesClass = defaultConnectionSource.getSettings().getConnectionSourcesClass();

        if(connectionSourcesClass == null) {
            return new InMemoryConnectionSources(defaultConnectionSource, connectionSourceFactory, configuration);
        }
        else {
            try {
                return connectionSourcesClass.newInstance(defaultConnectionSource, connectionSourceFactory, configuration)
            } catch (Throwable e) {
                throw new ConfigurationException("Cannot instantiate custom ConnectionSources implementation: $e.message", e)
            }
        }
    }
}
