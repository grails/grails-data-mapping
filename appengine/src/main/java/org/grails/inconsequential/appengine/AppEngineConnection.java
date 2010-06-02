package org.grails.inconsequential.appengine;

import org.grails.inconsequential.core.Connection;
import org.grails.inconsequential.core.DatastoreContext;
import org.grails.inconsequential.kv.mapping.Family;
import org.grails.inconsequential.kv.mapping.KeyValue;
import org.grails.inconsequential.kv.mapping.KeyValueMappingContext;
import org.grails.inconsequential.mapping.MappingContext;

import java.util.Map;

/**
 * Google App Engine connection to the datastore
 *
 * @author Guillaume Laforge
 */
public class AppEngineConnection implements Connection {
    private Map<String, String> connectionDetails;
    protected MappingContext<Family, KeyValue> mappingContext;

    /**
     * Create a new Google App Engine connection to the datastore.
     *
     * @param connectionDetails the connection details
     * @param mappingContext The Mapping Context
     */
    public AppEngineConnection(Map<String, String> connectionDetails, MappingContext<Family, KeyValue> mappingContext) {
        this.connectionDetails = connectionDetails;
        this.mappingContext = mappingContext;
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

    public DatastoreContext createContext() {
        AppEngineContext engineContext = new AppEngineContext(this);
        return engineContext;
    }
}
