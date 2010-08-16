package org.springframework.datastore.core;

import org.springframework.datastore.engine.EntityInterceptorAware;
import org.springframework.datastore.engine.Persister;
import org.springframework.datastore.mapping.MappingContext;
import org.springframework.datastore.query.Query;
import org.springframework.datastore.tx.Transaction;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * The Session represents the active interaction with a datastore. 
 *
 * @author Graeme Rocher
 * @author Guillaume Laforge
 *
 * @since 1.0
 */
public interface Session<N> extends EntityInterceptorAware {

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
     * Persists several objects returning their identifiers in the order specified by the Iterable
     *
     * @param objects The Objects
     * @return The identifiers
     */
    List<Serializable> persist(Iterable objects);

    /**
     * Retrieves an individual object
     *
     * @param type The ty
     * @param key The key
     * @return The object
     */
    <T> T retrieve(Class<T> type, Serializable key);

    /**
     * Deletes one or many objects
     *
     * @param objects The objects to delete
     */
    void delete(Iterable objects);

    /**
     * Deletes a single object
     * @param obj The object to delete
     */
    void delete(Object obj);

    /**
     * Retrieves several objects for the specified keys
     * @param type The type
     * @param keys The keys
     * @return A list of objects
     */
    List retrieveAll(Class type, Iterable keys);


    /**
     * Creates a query instance for the give type
     *
     * @param type The type
     * @return The query
     */
    Query createQuery(Class type);

    /**
     * @return The native interface to the datastore
     */
    N getNativeInterface();

    Persister getPersister(Object o);
}
