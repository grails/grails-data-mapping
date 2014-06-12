package org.grails.datastore.mapping.query.api;

import org.grails.datastore.mapping.core.Session;
import org.grails.datastore.mapping.query.Query;

/**
 * @author Graeme Rocher
 * @since 3.0.7
 */
public interface QueryAliasAwareSession extends Session {

    /**
     * Creates a query instance for the give type
     *
     * @param type The type
     * @param alias The alias to use in the query
     * @return The query
     */
    Query createQuery(Class type, String alias);
}
