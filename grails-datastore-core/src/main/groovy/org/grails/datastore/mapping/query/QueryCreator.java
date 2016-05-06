package org.grails.datastore.mapping.query;

/**
 * Query for any class that creates Queries
 *
 * @author Graeme Rocher
 * @since 6.0
 */
public interface QueryCreator {
    /**
     * Creates a query instance for the give type
     *
     * @param type The type
     * @return The query
     */
    Query createQuery(Class type);

    /**
     * @return Whether schemaless queries are allowed
     */
    boolean isSchemaless();
}
