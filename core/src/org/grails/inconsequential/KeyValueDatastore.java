package org.grails.inconsequential;

import java.util.List;
import java.util.Map;

/**
 * @author Guillaume Laforge
 */
public interface KeyValueDatastore<T> extends Datastore {
    /**
     * Store data in the datastore.
     *
     * @param ctxt The context under which the store operation is done
     * @param table The table, bucket, group in which the object should be stored
     * @param object A Map representing the data to be stored
     * @return the key under which the object was stored
     */
    public Key<T> store(Context ctxt, String table, Map object);

    /**
     * Retrieve an element from the datastore identified by their keys.
     *
     * @param ctxt The context under which the retrieve operation is done
     * @param key The key we won't to retrieve from the datastore
     * @return the property / value pairs representing the element retrieved
     */
    public Map<String, Object> retrieve(Context ctxt, Key<T> key);

    /**
     * Retrieve one or more elements from the datastore identified by their keys.
     *
     * @param ctxt The context under which the retrieve operation is done
     * @param keys The array of keys we won't to retrieve from the datastore
     * @return a list of property / value pairs representing the elements retrieved
     */
    public List<Map<String, Object>> retrieve(Context ctxt, Key<T>... keys);

    /**
     * Delete one or more elements from the datastore identified by their keys.
     *
     * @param ctxt The context under which the retrieve operation is done
     * @param keys The array of keys we won't to delete from the datastore
     */
    public void delete(Context ctxt, Key<T>... keys);

}