package org.springframework.datastore.mapping.appengine;

import java.util.Map;

import org.springframework.datastore.mapping.core.AbstractDatastore;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext;

/**
 * @author Graeme Rocher
 * @author Guillaume Laforge
 * @since 1.0
 */
public class AppEngineDatastore extends AbstractDatastore {

    // hard coded value of "gae" used for the keyspace since GAE manages spaces automatically
    public AppEngineDatastore() {
        super(new KeyValueMappingContext("gae"));
    }

    @Override
    protected Session createSession(@SuppressWarnings("hiding") Map<String, String> connectionDetails) {
        return new AppEngineSession(this, getMappingContext());
    }
}
