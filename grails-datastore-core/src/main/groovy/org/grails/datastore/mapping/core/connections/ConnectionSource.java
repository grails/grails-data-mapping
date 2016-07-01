package org.grails.datastore.mapping.core.connections;


/**
 * Represents a connection source, which could be a SQL DataSource, a MongoClient etc.
 *
 * @author Graeme Rocher
 * @since 6.0
 */
public interface ConnectionSource<T, S extends ConnectionSourceSettings> {
    /**
     * The name of the default connection source
     */
    String DEFAULT = "DEFAULT";

    /**
     * Constance for a mapping to all connection sources
     */
    String ALL = "ALL";

    /**
     * @return The name of the connection source
     */
    String getName();

    /**
     * @return The underlying native connection source, for example a SQL {@link javax.sql.DataSource}
     */
    T getSource();

    /**
     * @return The settings for the {@link ConnectionSource}
     */
    S getSettings();

}
