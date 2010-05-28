package org.grails.inconsequential.core;

import org.grails.inconsequential.mapping.MappingContext;

import java.util.Map;

/**
 * The <code>Datastore</code> interface is the basic commom denominator all NoSQL databases should support:
 * <ul>
 *     <li>Storing data</li>
 *     <li>Retrieving one or more elements at a time, identified by their keys</li>
 *     <li>Deleting one or more elements</li>
 * </ul>
 *
 * @author Guillaume Laforge
 * @author Graeme Rocher
 *
 */
public interface Datastore {

    /**
     * Connects to a datastore using information from a Map,
     * such as a user / password pair, the URL of the datastore, etc. 
     *
     * @param connectionDetails The Map containing the connection details
     * @return the connection created using the provided connection details
     */
    public Connection connect(Map<String, String> connectionDetails);


    /**
     * Obtains the MappingContext object
     *
     * @return The MappingContext object
     */
    MappingContext getMappingContext();
}
