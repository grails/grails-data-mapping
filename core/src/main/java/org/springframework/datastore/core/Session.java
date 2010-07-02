package org.springframework.datastore.core;

import org.springframework.datastore.engine.Persister;
import org.springframework.datastore.mapping.MappingContext;
import org.springframework.datastore.tx.Transaction;

import java.io.Serializable;
import java.util.Map;

/**
 * The Session represents the active interaction with a datastore. 
 *
 * @author Graeme Rocher
 * @author Guillaume Laforge
 *
 * @since 1.0
 */
public interface Session {

    /**
     * @return the session details as map of parameter / value String pairs
     */
    Map<String, String> getDetails();

    /**
     * @return true if connected to the datastore
     */
    boolean isConnected();

    /**
     * Disconnects from the datastore.
     */
    void disconnect();

    /**
     * Starts a transaction
     * @return The transaction
     */
    Transaction beginTransaction();

   /**
     * Obtains the MappingContext instance
     *
     * @return The MappingContext
     */
    MappingContext getMappingContext();

    /**
     * Stores and object and returns its key
     *
     * @param o The object
     * @return The the generated key
     */
    Serializable persist(Object o);

    /**
     * Retrieves an individual object
     *
     * @param type The ty
     * @param key The key
     * @return The object
     */
    Object retrieve(Class type, Serializable key);

    /**
     * Deletes one or many objects
     *
     * @param objects The objects to delete
     */
    void delete(Object... objects);

    /**
     * Obtains a persister for the given object
     * @param o The object
     * @return A Persister or null
     */
    Persister getPersister(Object o);    
}
