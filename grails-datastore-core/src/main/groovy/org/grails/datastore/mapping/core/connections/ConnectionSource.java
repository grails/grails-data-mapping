package org.grails.datastore.mapping.core.connections;

import org.springframework.core.env.PropertyResolver;

/**
 * Represents a connection source, which could be a SQL DataSource, a MongoClient etc.
 *
 * @author Graeme Rocher
 * @since 6.0
 */
public interface ConnectionSource<T> {
    /**
     * The name of the default connection source
     */
    String DEFAULT = "DEFAULT";
    /**
     * @return The name of the connection source
     */
    String getName();

    /**
     * @return The underlying native connection source, for example a SQL {@link javax.sql.DataSource}
     */
    T getSource();

    /**
     * @return The configuration used to create the connection source
     */
    PropertyResolver getConfiguration();
}
