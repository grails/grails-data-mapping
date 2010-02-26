package org.inconsequential.appengine;

import com.google.appengine.api.datastore.*;
import org.inconsequential.Connection;
import org.inconsequential.Context;
import org.inconsequential.Datastore;

import java.util.*;

/**
 * @author Guillaume Laforge
 */
public class AppEngineDatastore implements Datastore {

    private DatastoreService service = DatastoreServiceFactory.getDatastoreService();

    public Connection connect(Map<String, String> connectionDetails) {
        return new AppEngineConnection(connectionDetails);
    }

    public Object store(Context ctxt, String table, Map object) {
        Entity entity = new Entity(table);
        Set keySet = object.keySet();
        for (Object aKeySet : keySet) {
            String propertyName = (String) aKeySet;
            Object value = object.get(propertyName);
            entity.setProperty(propertyName, value);
        }
        return service.put(entity);
    }

    public Map<String, Object> retrieve(Context ctxt, Object key) {
        try {
            Entity entity = service.get(objectToKey(key));
            return entity.getProperties();
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    public List<Map<String, Object>> retrieve(Context ctxt, Object... keys) {
        List<Key> keysList = new ArrayList<Key>();
        for (Object key : keys) {
            keysList.add(objectToKey(key));
        }

        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();

        Map<Key, Entity> keyEntityMap = service.get(keysList);
        Set<Key> keySet = keyEntityMap.keySet();
        for (Key aKeySet : keySet) {
            Entity value = keyEntityMap.get(aKeySet);
            results.add(value.getProperties());
        }

        return results;
    }

    public void delete(Context ctxt, Object... keys) {
        List<Key> keysList = new ArrayList<Key>();
        for (Object key : keys) {
            keysList.add(objectToKey(key));
        }
        service.delete(keysList);
    }

    private static Key objectToKey(Object obj) {
        return obj instanceof Key ? (Key)obj : KeyFactory.stringToKey(obj.toString());
    }

}
