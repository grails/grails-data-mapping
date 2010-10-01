/* Copyright (C) 2010 SpringSource
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.datastore.mapping.core;

import org.springframework.datastore.mapping.engine.EntityInterceptorAware;
import org.springframework.datastore.mapping.engine.Persister;
import org.springframework.datastore.mapping.model.MappingContext;
import org.springframework.datastore.mapping.query.Query;
import org.springframework.datastore.mapping.transactions.Transaction;

import javax.persistence.FlushModeType;
import java.io.Serializable;
import java.util.List;

/**
 * The Session represents the active interaction with a datastore. 
 *
 * @author Graeme Rocher
 * @author Guillaume Laforge
 *
 * @since 1.0
 */
public interface Session extends EntityInterceptorAware {


    /**
     * Associates an attribute with the given persistent entity. Attributes will
     * be cleared out when the Session is closed or cleared.
     *
     * @param entity The persistent instance (must be associated with this Session)
     * @param attributeName The attribute name
     * @param value The value
     */
    void setAttribute(Object entity, String attributeName, Object value);

    /**
     * Obtains an attribute for the given entity
     *
     * @param entity The entity
     * @param attributeName The attribute
     * @return The attribute value
     */
    Object getAttribute(Object entity, String attributeName);
    
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
     * Refreshes the given objects state
     * @param o The object to refresh
     */
    void refresh(Object o);

    /**
     * Attaches an object the current session
     * @param o The object to attach
     */
    void attach(Object o);

    /**
     * Flushes any pending changes to the datastore
     */
    void flush();


    /**
     * Clears any pending changes to the datastore
     */
    void clear();

    /**
     * Clear a specific object
     * @param o The object to clear
     */
    void clear(Object o);

    /**
     * Whether the object is contained within the first level cache
     * @param o The object to check
     * @return True if it is
     */
    boolean contains(Object o);

    /**
     * The flush mode, defaults to FlushModeType.AUTO
     *
     * @param flushMode The FlushModeType
     */
    void setFlushMode(FlushModeType flushMode);

    /**
     * Obtains the current FlushModeType
     * @return The FlushModeType instance
     */
    FlushModeType getFlushMode();

    /**
     * Obtains a write lock on the given object
     *
     * @param o The object to lock
     */
    void lock(Object o);

    /**
     * Releases a lock, if not called all locked objects should be released by {@link #disconnect()}
     * 
     * @param o The object to unlock
     */
    void unlock(Object o);

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
     * @param type The type
     * @param key The key
     * @return The object
     */
    <T> T retrieve(Class<T> type, Serializable key);


    /**
     * Retrieves a proxy for the given key
     *
     * @param type The type
     * @param key The key
     * @return The object
     */
    <T> T proxy(Class<T> type, Serializable key);

    /**
     * Retrieves an individual object, using a write lock to prevent loss of updates
     *
     * @param type The type
     * @param key The key
     * @return The object
     */
    <T> T lock(Class<T> type, Serializable key);    

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
     * Retrieves several objects for the specified keys
     * @param type The type
     * @param keys The keys
     * @return A list of objects
     */
    List retrieveAll(Class type, Serializable...keys);

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
    Object getNativeInterface();

    /**
     * The persister for the given object
     * @param o The object
     * @return The persister
     */
    Persister getPersister(Object o);

    /**
     * Obtains the current transaction instance
     * @return The Transaction instance
     */
    Transaction getTransaction();

    /**
     * The Datastore that created this Session
     * @return The Datastore instance
     */
    Datastore getDatastore();


}
