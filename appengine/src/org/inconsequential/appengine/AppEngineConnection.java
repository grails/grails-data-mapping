package org.inconsequential.appengine;

import org.inconsequential.Connection;

import java.util.Map;

/**
 * Google App Engine connection to the datastore
 *
 * @author Guillaume Laforge
 */
public class AppEngineConnection implements Connection {
    private Map<String, String> connectionDetails;

    /**
     * Create a new Google App Engine connection to the datastore.
     *
     * @param connectionDetails the connection details
     */
    public AppEngineConnection(Map<String, String> connectionDetails) {
        this.connectionDetails = connectionDetails;
    }

    /**
     * @return the specific connection details
     */
    public Map<String, String> getDetails() {
        return connectionDetails;
    }

    /**
     * @return always true, always connected to the Google App Engine datastore
     */
    public boolean isConnected() {
        return true;
    }

    public void disconnect() {
        // No-op, always connected to the datastore.
    }
}
