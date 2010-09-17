package org.springframework.datastore.mapping.appengine;

import org.springframework.datastore.mapping.core.AbstractDatastore;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.keyvalue.mapping.KeyValueMappingContext;

import java.util.Map;

/**
 *
 * @author Graeme Rocher
 * @author Guillaume Laforge
 * 
 * @since 1.0
 */
public class AppEngineDatastore extends AbstractDatastore {

    
    // hard coded value of "gae" used for the keyspace since GAE manages spaces automatically
    public AppEngineDatastore() {
        super(new KeyValueMappingContext("gae"));
    }

    @Override
    protected Session createSession(Map<String, String> connectionDetails) {
        return new AppEngineSession(this,getMappingContext());
    }

}
