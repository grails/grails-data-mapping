package org.springframework.datastore.appengine;

import com.google.appengine.api.datastore.*;
import org.springframework.datastore.appengine.engine.AppEngineEntityPersister;
import org.springframework.datastore.core.AbstractSession;
import org.springframework.datastore.engine.Persister;
import org.springframework.datastore.keyvalue.KeyValueSession;
import org.springframework.datastore.keyvalue.mapping.Family;
import org.springframework.datastore.keyvalue.mapping.KeyValue;
import org.springframework.datastore.mapping.MappingContext;
import org.springframework.datastore.mapping.PersistentEntity;
import org.springframework.datastore.transactions.Transaction;

import java.util.*;

/**
 * Google App Engine session to the datastore
 *
 * @author Graeme Rocher
 * @author Guillaume Laforge
 *
 * @since 1.0
 */
public class AppEngineSession extends AbstractSession<DatastoreService> implements KeyValueSession<Key> {
    protected DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
    private AppEngineTransaction transaction;

    /**
     * Create a new Google App Engine session to the datastore.
     *
     * @param connectionDetails the session details
     * @param mappingContext The Mapping Context
     */
    public AppEngineSession(Map<String, String> connectionDetails, MappingContext<Family, KeyValue> mappingContext) {
        super(connectionDetails, mappingContext);
    }


    public Key store(String table, Map object) {
        Entity entity = new Entity(table);
        Set keySet = object.keySet();
        for (Object aKeySet : keySet) {
            String propertyName = (String) aKeySet;
            Object value = object.get(propertyName);
            entity.setProperty(propertyName, value);
        }
        return datastoreService.put(entity);
    }

    public Map<String, Object> retrieve(Key key) {
        try {
            Entity entity = datastoreService.get(key);
            return entity.getProperties();
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    public List<Map<String, Object>> retrieve(Key... keys) {
        List<com.google.appengine.api.datastore.Key> keysList = new ArrayList<Key>();
        keysList.addAll(Arrays.asList(keys));

        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();

        Map<com.google.appengine.api.datastore.Key, Entity> keyEntityMap = datastoreService.get(keysList);
        Set<com.google.appengine.api.datastore.Key> keySet = keyEntityMap.keySet();
        for (com.google.appengine.api.datastore.Key aKeySet : keySet) {
            Entity value = keyEntityMap.get(aKeySet);
            results.add(value.getProperties());
        }

        return results;
    }

    public void delete(Key... keys) {
        datastoreService.delete(Arrays.asList(keys));
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
    protected Transaction beginTransactionInternal() {
        AppEngineTransaction engineTransaction = new AppEngineTransaction(DatastoreServiceFactory.getDatastoreService().beginTransaction());
        this.transaction = engineTransaction;
        return engineTransaction;
    }

    public DatastoreService getNativeInterface() {
        return this.datastoreService;
    }

    @Override
    protected Persister createPersister(Class cls, MappingContext mappingContext) {
      PersistentEntity entity = mappingContext.getPersistentEntity(cls.getName());
        if(entity != null) {
            return new AppEngineEntityPersister(mappingContext, entity,this, datastoreService);
        }
        return null;
    }


}
