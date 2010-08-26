package org.springframework.datastore.keyvalue;

import org.springframework.datastore.core.Session;

import java.util.List;
import java.util.Map;

/**
 * @author Guillaume Laforge
 * @author Graeme Rocher
 *
 * @since 1.0
 */
public interface KeyValueSession<T> extends Session {
    /**
     * Store data in the datastore.
     *
     * @param table The table, bucket, group in which the object should be stored
     * @param object A Map representing the data to be stored
     * @return the key under which the object was stored
     */
    public T store(String table, Map object);

    /**
     * Retrieve an element from the datastore identified by their keys.
     *
     * @param key The key we won't to retrieveEntity from the datastore
     * @return the property / value pairs representing the element retrieved
     */
    public Map<String, Object> retrieve(T key);

    /**
     * Retrieve one or more elements from the datastore identified by their keys.
     *
     * @param keys The array of keys we won't to retrieveEntity from the datastore
     * @return a list of property / value pairs representing the elements retrieved
     */
    public List<Map<String, Object>> retrieve(T... keys);

}