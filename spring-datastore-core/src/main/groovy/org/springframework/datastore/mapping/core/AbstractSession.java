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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.persistence.FlushModeType;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.datastore.mapping.collection.PersistentCollection;
import org.springframework.datastore.mapping.core.impl.PendingInsert;
import org.springframework.datastore.mapping.core.impl.PendingOperation;
import org.springframework.datastore.mapping.core.impl.PendingOperationExecution;
import org.springframework.datastore.mapping.core.impl.PendingUpdate;
import org.springframework.datastore.mapping.engine.EntityPersister;
import org.springframework.datastore.mapping.engine.NativeEntryEntityPersister;
import org.springframework.datastore.mapping.engine.NonPersistentTypeException;
import org.springframework.datastore.mapping.engine.Persister;
import org.springframework.datastore.mapping.model.MappingContext;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.query.Query;
import org.springframework.datastore.mapping.transactions.Transaction;
import org.springframework.transaction.NoTransactionException;
import org.springframework.util.Assert;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap.Builder;
import com.googlecode.concurrentlinkedhashmap.EvictionListener;

/**
 * Abstract implementation of the {@link org.springframework.datastore.mapping.core.Session} interface that uses
 * a list of {@link org.springframework.datastore.mapping.engine.Persister} instances
 * to save, update and delete instances
 *
 * @author Graeme Rocher
 * @since 1.0
 * @param <N>
 */
public abstract class AbstractSession<N> extends AbstractAttributeStoringSession implements SessionImplementor {

    private static final EvictionListener<PersistentEntity, Collection<PendingInsert>> EXCEPTION_THROWING_INSERT_LISTENER =
            new EvictionListener<PersistentEntity, Collection<PendingInsert>>() {
        public void onEviction(PersistentEntity key, Collection<PendingInsert> value) {
            throw new DataAccessResourceFailureException("Maximum number (5000) of insert operations to flush() exceeded. Flush the session periodically to avoid this error for batch operations.");
        }
    };

    private static final EvictionListener<PersistentEntity, Collection<PendingUpdate>> EXCEPTION_THROWING_UPDATE_LISTENER =
            new EvictionListener<PersistentEntity, Collection<PendingUpdate>>() {
        public void onEviction(PersistentEntity key, Collection<PendingUpdate> value) {
            throw new DataAccessResourceFailureException("Maximum number (5000) of update operations to flush() exceeded. Flush the session periodically to avoid this error for batch operations.");
        }
    };

    protected Map<Class, Persister> persisters = new ConcurrentHashMap<Class,Persister>();
    private MappingContext mappingContext;
    protected ConcurrentLinkedQueue lockedObjects = new ConcurrentLinkedQueue();
    private Transaction transaction;
    private Datastore datastore;
    private FlushModeType flushMode = FlushModeType.AUTO;
    protected Map<Class, Map<Serializable, Object>> firstLevelCache = new ConcurrentHashMap<Class, Map<Serializable, Object>>();
    protected Map<Class, Map<Serializable, Object>> firstLevelEntryCache = new ConcurrentHashMap<Class, Map<Serializable, Object>>();
    protected Map<CollectionKey, Collection> firstLevelCollectionCache = new ConcurrentHashMap<CollectionKey, Collection>();

    protected Map<Object, Serializable> objectToKey = new ConcurrentHashMap<Object, Serializable>();

    private Map<PersistentEntity, Collection<PendingInsert>> pendingInserts =
        new Builder<PersistentEntity, Collection<PendingInsert>>()
           .listener(EXCEPTION_THROWING_INSERT_LISTENER)
           .maximumWeightedCapacity(5000).build();

    private Map<PersistentEntity, Collection<PendingUpdate>> pendingUpdates =
        new Builder<PersistentEntity, Collection<PendingUpdate>>()
           .listener(EXCEPTION_THROWING_UPDATE_LISTENER)
           .maximumWeightedCapacity(5000).build();

    protected Collection<Runnable> pendingDeletes = new ConcurrentLinkedQueue<Runnable>();
    protected Collection<Runnable> postFlushOperations = new ConcurrentLinkedQueue<Runnable>();
    private boolean exceptionOccurred;
    protected ApplicationEventPublisher publisher;

    public AbstractSession(Datastore datastore, MappingContext mappingContext,
               ApplicationEventPublisher publisher) {
        this.mappingContext = mappingContext;
        this.datastore = datastore;
        this.publisher = publisher;
    }

    public void addPostFlushOperation(Runnable runnable) {
        if (runnable != null && !postFlushOperations.contains(runnable)) {
            postFlushOperations.add(runnable);
        }
    }

    public void addPendingInsert(PendingInsert insert) {

        Collection<PendingInsert> inserts = pendingInserts.get(insert.getEntity());
        if (inserts == null) {
            inserts = new ConcurrentLinkedQueue<PendingInsert>();
            pendingInserts.put(insert.getEntity(), inserts);
        }

        inserts.add(insert);
    }

    public void addPendingUpdate(PendingUpdate update) {
        Collection<PendingUpdate> inserts = pendingUpdates.get(update.getEntity());
        if (inserts == null) {
            inserts = new ConcurrentLinkedQueue<PendingUpdate>();
            pendingUpdates.put(update.getEntity(), inserts);
        }

        inserts.add(update);
    }

    public Object getCachedEntry(PersistentEntity entity, Serializable key) {
        if (key == null) {
            return null;
        }

        return getEntryCache(entity.getJavaClass()).get(key);
    }

    public void cacheEntry(PersistentEntity entity, Serializable key, Object entry) {
        if (key == null || entry == null) {
            return;
        }

        getEntryCache(entity.getJavaClass()).put(key, entry);
    }

    public Collection getCachedCollection(PersistentEntity entity, Serializable key, String name) {
        if (key == null || name == null) {
            return null;
        }

        return firstLevelCollectionCache.get(
                new CollectionKey(entity.getJavaClass(), key, name));
    }

    public void cacheCollection(PersistentEntity entity, Serializable key, Collection collection, String name) {
        if (key == null || collection == null || name == null) {
            return;
        }

        firstLevelCollectionCache.put(
                new CollectionKey(entity.getJavaClass(), key, name),
                collection);
    }

    public Map<PersistentEntity, Collection<PendingInsert>> getPendingInserts() {
        return pendingInserts;
    }

    public Map<PersistentEntity, Collection<PendingUpdate>> getPendingUpdates() {
        return pendingUpdates;
    }

    public Collection<Runnable> getPendingDeletes() {
        return pendingDeletes;
    }

    public FlushModeType getFlushMode() {
        return flushMode;
    }

    public void setFlushMode(FlushModeType flushMode) {
        this.flushMode = flushMode;
    }

    public Datastore getDatastore() {
        return datastore;
    }

    public MappingContext getMappingContext() {
        return mappingContext;
    }

    public void flush() {
        if (exceptionOccurred) {
            throw new InvalidDataAccessResourceUsageException(
                 "Do not flush() the Session after an exception occurs");
        }

        boolean hasInserts = hasUpdates();
        if (!hasInserts) {
            return;
        }

        flushPendingInserts(pendingInserts);
        pendingInserts.clear();

        flushPendingUpdates(pendingUpdates);
        pendingUpdates.clear();

        executePendings(pendingDeletes);

        handleDirtyCollections();
        firstLevelCollectionCache.clear();

        executePendings(postFlushOperations);

        postFlush(hasInserts);
    }

    public boolean isDirty(Object instance) {

        if (instance == null) {
            return false;
        }

        EntityPersister persister = (EntityPersister) getPersister(instance);
        if (!(persister instanceof NativeEntryEntityPersister)) {
            return false;
        }

        Serializable id = persister.getObjectIdentifier(instance);
        if (id == null) {
            // not persistent
            return false;
        }

        Object entry = getEntryCache(instance.getClass()).get(id);
        return ((NativeEntryEntityPersister)persister).isDirty(instance, entry);
    }

    private void handleDirtyCollections() {
        for (Map.Entry<CollectionKey, Collection> entry : firstLevelCollectionCache.entrySet()) {
            Collection collection = entry.getValue();
            if (!(collection instanceof PersistentCollection)) {
                continue;
            }
            PersistentCollection persistentCollection = (PersistentCollection)collection;
            if (!persistentCollection.isDirty()) {
                continue;
            }

//TODO once an instance is flushed, its collections need to be non-dirty

            CollectionKey key = entry.getKey();
            Object owner = getInstanceCache(key.clazz).get(key.key);
            boolean d = isDirty(owner);
            System.out.println("TODO - UPDATE " + key);
        }
    }

    /**
     * The default implementation of flushPendingUpdates is to iterate over each update operation
     * and execute them one by one. This may be suboptimal for stores that support batch update
     * operations. Subclasses can override this method to implement batch update more efficiently.
     *
     * @param updates
     */
    protected void flushPendingUpdates(Map<PersistentEntity, Collection<PendingUpdate>> updates) {
        for (Collection<PendingUpdate> pending : updates.values()) {
            flushPendingOperations(pending);
        }
    }

    /**
     * The default implementation of flushPendingInserts is to iterate over each insert operations
     * and execute them one by one. This may be suboptimal for stores that support batch insert
     * operations. Subclasses can override this method to implement batch insert more efficiently.
     *
     * @param inserts The insert operations
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void flushPendingInserts(Map<PersistentEntity, Collection<PendingInsert>> inserts) {
        for (Collection<PendingInsert> pending : inserts.values()) {
            flushPendingOperations(pending);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void flushPendingOperations(Collection operations) {
        for (Object o : operations) {
            PendingOperation pendingOperation = (PendingOperation) o;
            try {
                PendingOperationExecution.executePendingOperation(pendingOperation);
            } catch (RuntimeException e) {
                exceptionOccurred = true;
                throw e;
            }
        }
    }

    private boolean hasUpdates() {
        return !pendingInserts.isEmpty() || !pendingUpdates.isEmpty() || !pendingDeletes.isEmpty();
    }

    protected void postFlush(@SuppressWarnings("unused") boolean hasUpdates) {
        // do nothing
    }

    private void executePendings(Collection<Runnable> pendings) {
        try {
            for (Runnable pending : pendings) {
                pending.run();
            }
        } catch (RuntimeException e) {
            exceptionOccurred = true;
            throw e;
        }
        pendings.clear();
    }

    public void clear() {
        for (Map<Serializable, Object> cache : firstLevelCache.values()) {
            cache.clear();
        }
        for (Map<Serializable, Object> cache : firstLevelEntryCache.values()) {
            cache.clear();
        }
        firstLevelCollectionCache.clear();
        pendingInserts.clear();
        pendingUpdates.clear();
        pendingDeletes.clear();
        attributes.clear();
        exceptionOccurred = false;
    }

    public final Persister getPersister(Object o) {
        if (o == null) return null;
        Class cls;
        if (o instanceof Class) {
            cls = (Class) o;
        }
        else if (o instanceof PersistentEntity) {
            cls = ((PersistentEntity)o).getJavaClass();
        }
        else {
            cls = o.getClass();
        }
        Persister p = persisters.get(cls);
        if (p == null) {
            p = createPersister(cls, getMappingContext());
            firstLevelCache.put(cls, new ConcurrentHashMap<Serializable, Object>());
            if (p != null) {
                persisters.put(cls, p);
            }
        }
        return p;
    }

    @SuppressWarnings("hiding")
    protected abstract Persister createPersister(Class cls, MappingContext mappingContext);

    public boolean contains(Object o) {
        if (o == null) {
            return false;
        }

        return getInstanceCache(o.getClass()).containsValue(o);
    }

    public boolean isCached(Class type, Serializable key) {
        if (type == null || key == null) {
            return false;
        }

        return getInstanceCache(type).containsKey(key);
    }

    public void cacheInstance(Class type, Serializable key, Object instance) {
        if (type == null || key == null || instance == null) {
            return;
        }

        getInstanceCache(type).put(key, instance);
    }

    public Object getCachedInstance(Class type, Serializable key) {
        if (type == null || key == null) {
            return false;
        }

        return getInstanceCache(type).get(key);
    }

    public void clear(Object o) {
        if (o == null) {
            return;
        }

        final Map<Serializable, Object> cache = firstLevelCache.get(o.getClass());
        if (cache != null) {
            Serializable key = objectToKey.get(o);
            if (key != null) {
                cache.remove(key);
                objectToKey.remove(o);
            }
        }

        attributes.remove(o);
    }

    public void attach(Object o) {
        if (o == null) {
            return;
        }

        EntityPersister p = (EntityPersister) getPersister(o);
        if (p == null) {
            return;
        }

        Serializable identifier = p.getObjectIdentifier(o);
        if (identifier != null) {
            cacheObject(identifier, o);
        }
    }

    protected void cacheObject(Serializable identifier, Object o) {
        if (identifier == null || o == null) {
            return;
        }

        objectToKey.put(o, identifier);
        getInstanceCache(o.getClass()).put(identifier, o);
    }

    public Serializable persist(Object o) {
        Assert.notNull(o, "Cannot persist null object");
        Persister persister = getPersister(o);
        if (persister == null) {
            throw new NonPersistentTypeException("Object [" + o +
                    "] cannot be persisted. It is not a known persistent type.");
        }

        final Serializable key = persister.persist(o);
        cacheObject(key, o);
        return key;
    }

    public void refresh(Object o) {
        Assert.notNull(o, "Cannot persist null object");
        Persister persister = getPersister(o);
        if (persister == null) {
            throw new NonPersistentTypeException("Object [" + o +
                    "] cannot be refreshed. It is not a known persistent type.");
        }

        final Serializable key = persister.refresh(o);
        cacheObject(key, o);
    }

    public Object retrieve(Class type, Serializable key) {
        if (key == null || type == null) {
            return null;
        }

        Persister persister = getPersister(type);
        if (persister == null) {
            throw new NonPersistentTypeException("Cannot retrieve object with key [" + key +
                    "]. The class [" + type.getName() + "] is not a known persistent type.");
        }

        final PersistentEntity entity = getMappingContext().getPersistentEntity(type.getName());
        if (entity != null) {
            key = (Serializable) getMappingContext().getConversionService().convert(
                    key, entity.getIdentity().getType());
        }

        Object o = getInstanceCache(type).get(key);
        if (o == null) {
            o = persister.retrieve(key);
            if (o != null) {
                cacheObject(key, o);
            }
        }
        return o;
    }

    public Object proxy(Class type, Serializable key) {
        if (key == null || type == null) {
            return null;
        }

        Persister persister = getPersister(type);
        if (persister == null) {
            throw new NonPersistentTypeException("Cannot retrieve object with key [" + key +
                    "]. The class [" + type.getName() + "] is not a known persistent type.");
        }

        return persister.proxy(key);
    }

    public void lock(Object o) {
        throw new UnsupportedOperationException("Datastore ["+getClass().getName()+"] does not support locking.");
    }

    public Object lock(Class type, Serializable key) {
        throw new UnsupportedOperationException("Datastore ["+getClass().getName()+"] does not support locking.");
    }

    public void unlock(Object o) {
        if (o != null) {
            lockedObjects.remove(o);
        }
    }

    public void delete(final Object obj) {
        if (obj == null) {
            return;
        }

        getPendingDeletes().add(new Runnable() {
            public void run() {
                Persister p = getPersister(obj);
                if (p == null) {
                    return;
                }

                p.delete(obj);
                clear(obj);
            }
        });
    }

    public void delete(final Iterable objects) {
        if (objects == null) {
            return;
        }

        for (Object object : objects) {
            if (object == null) {
                continue;
            }
            final Persister p = getPersister(object);
            if (p == null) {
                continue;
            }

            pendingDeletes.add(new Runnable() {
                public void run() {
                    p.delete(objects);
                    for (Object o : objects) {
                        clear(o);
                    }
                }
            });
        }
    }

    public List<Serializable> persist(Iterable objects) {
        if (objects == null) {
            return Collections.emptyList();
        }

        final Iterator i = objects.iterator();
        if (!i.hasNext()) {
            return Collections.emptyList();
        }

        // peek at the first object to get the persister
        final Object obj = i.next();
        final Persister p = getPersister(obj);
        if (p == null) {
            throw new NonPersistentTypeException("Cannot persist objects. The class [" +
                     obj.getClass().getName() + "] is not a known persistent type.");
        }

        return p.persist(objects);
    }

    public List retrieveAll(Class type, Iterable keys) {
        Persister p = getPersister(type);
        if (p == null) {
            throw new NonPersistentTypeException("Cannot retrieve objects with keys [" + keys +
                    "]. The class [" + type.getName() + "] is not a known persistent type.");
        }

        return p.retrieveAll(keys);
    }

    public List retrieveAll(Class type, Serializable... keys) {
        Persister p = getPersister(type);
        if (p == null) {
            throw new NonPersistentTypeException("Cannot retrieve objects with keys [" + keys +
                    "]. The class [" + type.getName() + "] is not a known persistent type.");
        }

        List retrieved = new ArrayList(keys.length);
        final Map<Serializable, Object> cache = getInstanceCache(type);
        for (int i = 0; i < keys.length; i++) {
            Serializable key = keys[i];
            Object cached = cache.get(key);
            if (cached != null) {
                retrieved.add(i, cached);
            }
            else {
                Object loaded = retrieve(type, key);
                retrieved.add(i, loaded);
                cache.put(key, loaded);
            }
        }
        return retrieved;
    }

    public Query createQuery(Class type) {
        Persister p = getPersister(type);
        if (p == null) {
            throw new NonPersistentTypeException("Cannot create query. The class [" + type +
                    "] is not a known persistent type.");
        }

        return p.createQuery();
    }

    public final Transaction beginTransaction() {
        transaction = beginTransactionInternal();
        return transaction;
    }

    protected abstract Transaction beginTransactionInternal();

    public Transaction getTransaction() {
        if (transaction == null) {
            throw new NoTransactionException("Transaction not started. Call beginTransaction() first");
        }
        return transaction;
    }

    private Map<Serializable, Object> getInstanceCache(Class c) {
        Map<Serializable, Object> cache = firstLevelCache.get(c);
        if (cache == null) {
            cache = new ConcurrentHashMap<Serializable, Object>();
            firstLevelCache.put(c, cache);
        }
        return cache;
    }

    private Map<Serializable, Object> getEntryCache(Class c) {
        Map<Serializable, Object> cache = firstLevelEntryCache.get(c);
        if (cache == null) {
            cache = new ConcurrentHashMap<Serializable, Object>();
            firstLevelEntryCache.put(c, cache);
        }
        return cache;
    }

    private static class CollectionKey {
        final Class clazz;
        final Serializable key;
        final String collectionName;

        private CollectionKey(Class clazz, Serializable key, String collectionName) {
            this.clazz = clazz;
            this.key = key;
            this.collectionName = collectionName;
        }

        @Override
        public int hashCode() {
            int value = 17;
            value = value * 37 + clazz.getName().hashCode();
            value = value * 37 + key.hashCode();
            value = value * 37 + collectionName.hashCode();
            return value;
        }

        @Override
        public boolean equals(Object obj) {
            CollectionKey other = (CollectionKey)obj;
            return other.clazz.getName() == clazz.getName() &&
                other.key.equals(key) &&
                other.collectionName.equals(collectionName);
        }

        @Override
        public String toString() {
            return clazz.getName() + ':' + key + ':' + collectionName;
        }
    }
}
