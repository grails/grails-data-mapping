package org.grails.datastore.mapping.core.connections;

import org.springframework.core.env.PropertyResolver;

/**
 * A factory for creating new {@link ConnectionSource} instances
 *
 * @author Graeme Rocher
 * @since 6.0
 */
public interface ConnectionSourceFactory<T> {

    /**
     * Creates a new {@link ConnectionSource} for the given name and configuration
     *
     * @param name The name
     * @param configuration The configuration
     * @return The {@link ConnectionSource}
     *
     * @throws org.grails.datastore.mapping.core.exceptions.ConfigurationException If there is an issue with the configuration
     */
    ConnectionSource<T> create(String name, PropertyResolver configuration);
}
