package org.grails.datastore.mapping.core.connections;

import org.grails.datastore.mapping.config.Settings;
import org.springframework.core.env.PropertyResolver;

import java.io.Serializable;

/**
 * A factory for creating new {@link ConnectionSource} instances
 *
 * @author Graeme Rocher
 * @since 6.0
 */
public interface ConnectionSourceFactory<T, S extends ConnectionSourceSettings> extends Settings {

    /**
     * Creates a new {@link ConnectionSource} for the given name and configuration
     *
     * @param name The name
     * @param configuration The configuration
     * @return The {@link ConnectionSource}
     *
     * @throws org.grails.datastore.mapping.core.exceptions.ConfigurationException If there is an issue with the configuration
     */
    ConnectionSource<T, S> create(String name, PropertyResolver configuration);


    /**
     * Creates a new {@link ConnectionSource} for the given name and configuration
     *
     * @param name The name
     * @param configuration The configuration
     * @param fallbackSettings The settings to fallback to if none are specified
     *
     * @return The {@link ConnectionSource}
     *
     * @throws org.grails.datastore.mapping.core.exceptions.ConfigurationException If there is an issue with the configuration
     */
    ConnectionSource<T, S> create(String name, PropertyResolver configuration, S fallbackSettings);

    /**
     * @return Obtain the prefix used to obtain the default configuration. For example "grails.mongodb" or "grails.neo4j"
     */
    Serializable getConnectionSourcesConfigurationKey();
}
