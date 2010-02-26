package org.grails.inconsequential;

import java.util.List;
import java.util.Map;

/**
 * The <code>Datastore</code> interface is the basic commom denominator all NoSQL databases should support:
 * <ul>
 *     <li>Storing data</li>
 *     <li>Retrieving one or more elements at a time, identified by their keys</li>
 *     <li>Deleting one or more elements</li>
 * </ul>
 *
 * @author Guillaume Laforge
 */
public interface Datastore {

    /**
     * Connects to a datastore using information from a Map,
     * such as a user / password pair, the URL of the datastore, etc. 
     *
     * @param connectionDetails The Map containing the connection details
     * @return the connection created using the provided connection details
     */
    public Connection connect(Map<String, String> connectionDetails);

    /**
     * Store data in the datastore.
     *
     * @param ctxt The context under which the store operation is done
     * @param table The table, bucket, group in which the object should be stored
     * @param object A Map representing the data to be stored
     * @return the key under which the object was stored
     */
    public Object store(Context ctxt, String table, Map object);

    /**
     * Retrieve an element from the datastore identified by their keys.
     *
     * @param ctxt The context under which the retrieve operation is done
     * @param key The key we won't to retrieve from the datastore
     * @return the property / value pairs representing the element retrieved
     */
    public Map<String, Object> retrieve(Context ctxt, Object key);

    /**
     * Retrieve one or more elements from the datastore identified by their keys.
     *
     * @param ctxt The context under which the retrieve operation is done
     * @param keys The array of keys we won't to retrieve from the datastore
     * @return a list of property / value pairs representing the elements retrieved
     */
    public List<Map<String, Object>> retrieve(Context ctxt, Object... keys);

    /**
     * Delete one or more elements from the datastore identified by their keys.
     *
     * @param ctxt The context under which the retrieve operation is done
     * @param keys The array of keys we won't to delete from the datastore
     */
    public void delete(Context ctxt, Object... keys);

}
