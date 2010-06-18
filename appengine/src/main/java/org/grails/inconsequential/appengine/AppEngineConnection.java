package org.grails.inconsequential.appengine;

import com.google.appengine.api.datastore.*;
import com.google.appengine.api.datastore.Key;
import org.grails.inconsequential.appengine.engine.AppEngineEntityPersister;
import org.grails.inconsequential.core.*;
import org.grails.inconsequential.engine.Persister;
import org.grails.inconsequential.kv.KeyValueDatastoreConnection;
import org.grails.inconsequential.kv.mapping.Family;
import org.grails.inconsequential.kv.mapping.KeyValue;
import org.grails.inconsequential.mapping.MappingContext;
import org.grails.inconsequential.mapping.PersistentEntity;
import org.grails.inconsequential.tx.*;
import org.grails.inconsequential.tx.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Google App Engine connection to the datastore
 *
 * @author Graeme Rocher
 * @author Guillaume Laforge
 *
 * @since 1.0
 */
public class AppEngineConnection extends AbstractObjectDatastoreConnection<com.google.appengine.api.datastore.Key> implements KeyValueDatastoreConnection<com.google.appengine.api.datastore.Key> {
    protected DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
    private AppEngineTransaction transaction;

    /**
     * Create a new Google App Engine connection to the datastore.
     *
     * @param connectionDetails the connection details
     * @param mappingContext The Mapping Context
     */
    public AppEngineConnection(Map<String, String> connectionDetails, MappingContext<Family, KeyValue> mappingContext) {
        super(connectionDetails, mappingContext);
    }

    public org.grails.inconsequential.core.Key<Key> createKey(Key nativeKey) {
        return new AppEngineKey(nativeKey);
    }

    public org.grails.inconsequential.core.Key<com.google.appengine.api.datastore.Key> store(String table, Map object) {
        Entity entity = new Entity(table);
        Set keySet = object.keySet();
        for (Object aKeySet : keySet) {
            String propertyName = (String) aKeySet;
            Object value = object.get(propertyName);
            entity.setProperty(propertyName, value);
        }
        return new AppEngineKey(datastoreService.put(entity));
    }

    public Map<String, Object> retrieve(org.grails.inconsequential.core.Key<Key> key) {
        try {
            Entity entity = datastoreService.get(key.getNativeKey());
            return entity.getProperties();
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    public List<Map<String, Object>> retrieve(org.grails.inconsequential.core.Key<Key>... keys) {
        List<com.google.appengine.api.datastore.Key> keysList = new ArrayList<Key>();
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

    public void delete(org.grails.inconsequential.core.Key<Key>... keys) {
        List<com.google.appengine.api.datastore.Key> keysList = new ArrayList<com.google.appengine.api.datastore.Key>();
        for (org.grails.inconsequential.core.Key<Key> key : keys) {
            keysList.add(key.getNativeKey());
        }

        datastoreService.delete(keysList);
    }

    /**
     * @return always true, always connected to the Google App Engine datastore
     */
    public boolean isConnected() {
        return true;
    }

    /**
     * Start a new transaction.
     *
     * @return a started transaction
     */
    public Transaction beginTransaction() {
        AppEngineTransaction engineTransaction = new AppEngineTransaction(DatastoreServiceFactory.getDatastoreService().beginTransaction());
        this.transaction = engineTransaction;
        return engineTransaction;
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
