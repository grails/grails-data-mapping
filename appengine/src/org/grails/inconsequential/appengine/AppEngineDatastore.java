package org.grails.inconsequential.appengine;

import com.google.appengine.api.datastore.*;
import org.grails.inconsequential.*;
import org.grails.inconsequential.Key;

import java.util.*;

/**
 * @author Guillaume Laforge
 */
public class AppEngineDatastore implements Datastore<com.google.appengine.api.datastore.Key> {

    private DatastoreService service = DatastoreServiceFactory.getDatastoreService();

    public Connection connect(Map<String, String> connectionDetails) {
        return new AppEngineConnection(connectionDetails);
    }

    public org.grails.inconsequential.Key store(Context ctxt, String table, Map object) {
        Entity entity = new Entity(table);
        Set keySet = object.keySet();
        for (Object aKeySet : keySet) {
            String propertyName = (String) aKeySet;
            Object value = object.get(propertyName);
            entity.setProperty(propertyName, value);
        }
        return new AppEngineKey(service.put(entity));
    }

    public Map<String, Object> retrieve(Context ctxt, Key<com.google.appengine.api.datastore.Key> key) {
        try {
            Entity entity = service.get(key.getNativeKey());
            return entity.getProperties();
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    public List<Map<String, Object>> retrieve(Context ctxt, Key<com.google.appengine.api.datastore.Key>... keys) {
        List<com.google.appengine.api.datastore.Key> keysList = new ArrayList<com.google.appengine.api.datastore.Key>();
        for (Key<com.google.appengine.api.datastore.Key> key : keys) {
            keysList.add(key.getNativeKey());
        }

        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();

        Map<com.google.appengine.api.datastore.Key, Entity> keyEntityMap = service.get(keysList);
        Set<com.google.appengine.api.datastore.Key> keySet = keyEntityMap.keySet();
        for (com.google.appengine.api.datastore.Key aKeySet : keySet) {
            Entity value = keyEntityMap.get(aKeySet);
            results.add(value.getProperties());
        }

        return results;
    }

    public void delete(Context ctxt, Key<com.google.appengine.api.datastore.Key>... keys) {
        List<com.google.appengine.api.datastore.Key> keysList = new ArrayList<com.google.appengine.api.datastore.Key>();
        for (Key<com.google.appengine.api.datastore.Key> key : keys) {
            keysList.add(key.getNativeKey());
        }
        
        service.delete(keysList);
    }
}
