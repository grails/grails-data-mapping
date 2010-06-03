package org.grails.inconsequential.appengine;

import com.google.appengine.api.datastore.*;
import com.google.appengine.api.datastore.Key;
import org.grails.inconsequential.core.*;
import org.grails.inconsequential.kv.KeyValueDatastoreConnection;
import org.grails.inconsequential.kv.mapping.Family;
import org.grails.inconsequential.kv.mapping.KeyValue;
import org.grails.inconsequential.kv.mapping.KeyValueMappingContext;
import org.grails.inconsequential.mapping.MappingContext;

import java.util.*;

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
    protected Connection createConnection(Map<String, String> connectionDetails) {
        return new AppEngineConnection(connectionDetails, getMappingContext());
    }

}
