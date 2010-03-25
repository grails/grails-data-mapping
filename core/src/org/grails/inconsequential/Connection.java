package org.grails.inconsequential;

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
     * Create a context of execution.
     *
     * @return an instance of Context.
     */
    Context createContext();
}
