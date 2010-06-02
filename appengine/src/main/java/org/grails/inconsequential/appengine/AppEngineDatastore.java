package org.grails.inconsequential.appengine;

import com.google.appengine.api.datastore.*;
import com.google.appengine.api.datastore.Key;
import org.grails.inconsequential.appengine.engine.AppEngineEntityPersister;
import org.grails.inconsequential.core.*;
import org.grails.inconsequential.engine.Persister;
import org.grails.inconsequential.kv.KeyValueDatastore;
import org.grails.inconsequential.kv.mapping.Family;
import org.grails.inconsequential.kv.mapping.KeyValue;
import org.grails.inconsequential.kv.mapping.KeyValueMappingContext;
import org.grails.inconsequential.mapping.MappingContext;
import org.grails.inconsequential.mapping.PersistentEntity;

import java.util.*;

/**
 *
 * @author Graeme Rocher
 * @author Guillaume Laforge
 * 
 * @since 1.0
 */
public class AppEngineDatastore extends AbstractObjectDatastore implements KeyValueDatastore<Key> {

    
    protected DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
    // hard coded value of "gae" used for the keyspace since GAE manages spaces automatically
    protected MappingContext<Family, KeyValue> mappingContext = new KeyValueMappingContext("gae");

    public Connection connect(Map<String, String> connectionDetails) {
        return new AppEngineConnection(connectionDetails, mappingContext);
    }

    public MappingContext<Family, KeyValue> getMappingContext() {
        return mappingContext;
    }

    public org.grails.inconsequential.core.Key<com.google.appengine.api.datastore.Key> store(DatastoreContext ctxt, String table, Map object) {
        Entity entity = new Entity(table);
        Set keySet = object.keySet();
        for (Object aKeySet : keySet) {
            String propertyName = (String) aKeySet;
            Object value = object.get(propertyName);
            entity.setProperty(propertyName, value);
        }
        return new AppEngineKey(datastoreService.put(entity));
    }

    public Map<String, Object> retrieve(DatastoreContext ctxt, org.grails.inconsequential.core.Key<Key> key) {
        try {
            Entity entity = datastoreService.get(key.getNativeKey());
            return entity.getProperties();
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    public List<Map<String, Object>> retrieve(DatastoreContext ctxt, org.grails.inconsequential.core.Key<Key>... keys) {
        List<com.google.appengine.api.datastore.Key> keysList = new ArrayList<com.google.appengine.api.datastore.Key>();
        for (org.grails.inconsequential.core.Key<com.google.appengine.api.datastore.Key> key : keys) {
            keysList.add(key.getNativeKey());
        }

        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();

        Map<com.google.appengine.api.datastore.Key, Entity> keyEntityMap = datastoreService.get(keysList);
        Set<com.google.appengine.api.datastore.Key> keySet = keyEntityMap.keySet();
        for (com.google.appengine.api.datastore.Key aKeySet : keySet) {
            Entity value = keyEntityMap.get(aKeySet);
            results.add(value.getProperties());
        }

        return results;
    }

    public void delete(DatastoreContext ctxt, org.grails.inconsequential.core.Key<Key>... keys) {
        List<com.google.appengine.api.datastore.Key> keysList = new ArrayList<com.google.appengine.api.datastore.Key>();
        for (org.grails.inconsequential.core.Key<Key> key : keys) {
            keysList.add(key.getNativeKey());
        }
        
        datastoreService.delete(keysList);
    }

    @Override
    protected Persister createPersister(Class cls, MappingContext mappingContext) {
        PersistentEntity entity = mappingContext.getPersistentEntity(cls.getName());
        if(entity != null) {
            return new AppEngineEntityPersister(entity, datastoreService);
        }
        return null;
    }
}
