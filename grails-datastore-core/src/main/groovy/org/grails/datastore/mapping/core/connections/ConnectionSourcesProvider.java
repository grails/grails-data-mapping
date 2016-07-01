package org.grails.datastore.mapping.core.connections;

/**
 * An interfaces for classes that provide {@link ConnectionSources}
 *
 * @author Graeme Rocher
 * @since 6.0
 */
public interface ConnectionSourcesProvider<T, S extends ConnectionSourceSettings> {

    /**
     * @return The {@link ConnectionSources}
     */
    public ConnectionSources<T, S> getConnectionSources();
}
