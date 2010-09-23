package org.springframework.datastore.mapping.node.engine;


import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.engine.*;
import org.springframework.datastore.mapping.keyvalue.convert.ByteArrayAwareTypeConverter;
import org.springframework.datastore.mapping.model.*;
import org.springframework.datastore.mapping.model.types.*;
import org.springframework.datastore.mapping.node.mapping.Node;
import org.springframework.datastore.mapping.node.mapping.NodeProperty;
import org.springframework.datastore.mapping.query.Query;

import java.io.Serializable;
import java.util.*;

/**
 * Abstract implementation of the EntityPersister abstract class
 * for Node style stores
 *
 * @author Erawat Chamanont
 * @since 1.0
 */
public abstract class AbstractNodeEntityPersister<T, K> extends LockableEntityPersister {
    protected Session session;
    protected ClassMapping classMapping;

    public AbstractNodeEntityPersister(MappingContext mappingContext, PersistentEntity entity, Session session) {
        super(mappingContext, entity, session);
        this.session = session;
        this.classMapping = entity.getMapping();
    }

    @Override
    protected List<Object> retrieveAllEntities(PersistentEntity persistentEntity, Serializable[] keys) {
        return null;
    }

    @Override
    protected List<Object> retrieveAllEntities(PersistentEntity persistentEntity, Iterable<Serializable> keys) {
        return null;
    }

    @Override
    protected List<Serializable> persistEntities(PersistentEntity persistentEntity, Iterable objs) {
        return null;
    }

    @Override
    protected Object retrieveEntity(PersistentEntity persistentEntity, Serializable key) {
        return null;
    }

    @Override
    protected Serializable persistEntity(PersistentEntity persistentEntity, Object obj) {
        return null;
    }

    @Override
    protected void deleteEntity(PersistentEntity persistentEntity, Object obj) {

    }

    @Override
    protected void deleteEntities(PersistentEntity persistentEntity, Iterable objects) {

    }

    @Override
    public Object lock(Serializable id) throws CannotAcquireLockException {
        return null;
    }

    @Override
    public Object lock(Serializable id, int timeout) throws CannotAcquireLockException {
        return null;
    }

    @Override
    public boolean isLocked(Object o) {
        return false;
    }

    @Override
    public void unlock(Object o) {

    }

    public Object proxy(Serializable key) {
        return null;
    }

    public Query createQuery() {
        return null;
    }

    public Serializable refresh(Object o) {
        return null;  
    }


      /**
     * Used to establish the native key to use from the identifier defined by the object
     *
     * @param identifier The identifier specified by the object
     * @return The native key which may just be a cast from the identifier parameter to K
     */
    protected K inferNativeKey(Object identifier) {
        return (K) identifier;
    }


    /**
     * Reads a value for the given key from the native entry
     *
     * @param nativeEntry The native entry. Could be a  JCR Node etc.
     * @param property    The property key
     * @return The value
     */
    protected abstract Object getEntryValue(T nativeEntry, String property);

    /**
     * Reads the native form of a Node datastore entry. This could be
     * a JCR Node, a Graph Nodeetc.
     *
     * @param persistentEntity The persistent entity
     * @param key              The key
     * @return The native form
     */
    protected abstract T retrieveEntry(PersistentEntity persistentEntity, Serializable key);


    /**
     * Creates a new entry for the given Node.
     *
     * @param name The Node name
     * @return An entry such as a JCR Node etc.
     */
    protected abstract T createNewEntry(String name);

    /**
     * Sets a value on an entry
     *
     * @param nativeEntry  The native entry such as a JCR Node etc.
     * @param propertyName The Property Name
     * @param value        The value
     */
    protected abstract void setEntryValue(T nativeEntry, String propertyName, Object value);

    /**
     * Stores the native form of a Node style datastore to the actual data store
     *
     * @param persistentEntity The persistent entity
     * @param nativeEntry      The native form. Could be a a JCR Node,etc.
     * @return The native identitys
     */
    protected abstract K storeEntry(PersistentEntity persistentEntity, T nativeEntry);

    /**
     * Updates an existing entry to the actual datastore
     *
     * @param persistentEntity The PersistentEntity
     * @param id               The id of the object to update
     * @param entry            The entry
     */
    protected abstract void updateEntry(PersistentEntity persistentEntity, K id, T entry);
}
