package org.grails.inconsequential.core;

import org.grails.inconsequential.tx.Transaction;

import java.util.Map;

/**
 * The Connection represents the active connection to the datastore. 
 *
 * @author Guillaume Laforge
 */
public interface Connection {

    /**
     * @return the connection details as map of parameter / value String pairs
     */
    Map<String, String> getDetails();

    /**
     * @return true if connected to the datastore
     */
    boolean isConnected();

    /**
     * Disconnects from the datastore.
     */
    void disconnect();

    /**
     * Starts a transaction
     * @return The transaction
     */
    Transaction beginTransaction();
}
