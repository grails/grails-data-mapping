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

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.datastore.mapping.core.impl.PendingInsert;
import org.springframework.datastore.mapping.core.impl.PendingOperation;
import org.springframework.datastore.mapping.core.impl.PendingOperationExecution;
import org.springframework.datastore.mapping.core.impl.PendingUpdate;
import org.springframework.datastore.mapping.engine.EntityInterceptor;
import org.springframework.datastore.mapping.engine.EntityInterceptorAware;
import org.springframework.datastore.mapping.engine.EntityPersister;
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
 */
public abstract class AbstractSession<N> extends AbstractAttributeStoringSession implements Session, SessionImplementor {
    private static final EvictionListener<PersistentEntity, Collection<PendingInsert>> EXCEPTION_THROWING_INSERT_LISTENER = new EvictionListener<PersistentEntity, Collection<PendingInsert>>() {
        public void onEviction(PersistentEntity key, Collection<PendingInsert> value) {
            throw new DataAccessResourceFailureException("Maximum number (5000) of insert operations to flush() exceeded. Flush the session periodically to avoid this error for batch operations.");
        }
    };
    private static final EvictionListener<PersistentEntity, Collection<PendingUpdate>> EXCEPTION_THROWING_UPDATE_LISTENER = new EvictionListener<PersistentEntity, Collection<PendingUpdate>>() {
        public void onEviction(PersistentEntity key, Collection<PendingUpdate> value) {
            throw new DataAccessResourceFailureException("Maximum number (5000) of update operations to flush() exceeded. Flush the session periodically to avoid this error for batch operations.");
        }
    };

    protected Map<Class,Persister> persisters = new ConcurrentHashMap<Class,Persister>();
    private MappingContext mappingContext;
    protected List<EntityInterceptor> interceptors = new ArrayList<EntityInterceptor>();
    protected ConcurrentLinkedQueue lockedObjects = new ConcurrentLinkedQueue();
    private Transaction transaction;
    private Datastore datastore;
    private FlushModeType flushMode = FlushModeType.AUTO;
    protected Map<Class, Map<Serializable, Object>> firstLevelCache = new ConcurrentHashMap<Class, Map<Serializable, Object>>();
    protected Map<Class, Map<Serializable, Object>> firstLevelEntryCache = new ConcurrentHashMap<Class, Map<Serializable, Object>>();

    protected Map<Object, Serializable> objectToKey = new ConcurrentHashMap<Object, Serializable>();
    private Map<PersistentEntity, Collection<PendingInsert>> pendingInserts = new Builder<PersistentEntity, Collection<PendingInsert>>()
                                                                            .listener(EXCEPTION_THROWING_INSERT_LISTENER)
                                                                            .maximumWeightedCapacity(5000).build();
    private Map<PersistentEntity, Collection<PendingUpdate>> pendingUpdates = new Builder<PersistentEntity, Collection<PendingUpdate>>()
                                                                            .listener(EXCEPTION_THROWING_UPDATE_LISTENER)
                                                                            .maximumWeightedCapacity(5000).build();

    protected Collection<Runnable> pendingDeletes = new ConcurrentLinkedQueue<Runnable>();
    protected Collection<Runnable> postFlushOperations = new ConcurrentLinkedQueue<Runnable>();
    private boolean exceptionOccurred;

    public AbstractSession(Datastore datastore,MappingContext mappingContext) {
        this.mappingContext = mappingContext;
        this.datastore = datastore;
    }

    public void addPostFlushOperation(Runnable runnable) {
        if (runnable != null) {
            if (!postFlushOperations.contains(runnable)) {
                postFlushOperations.add(runnable);
            }
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
        if (key != null) {
            final Map<Serializable, Object> map = firstLevelEntryCache.get(entity.getJavaClass());
            if (map != null) {
                return map.get(key);
            }
        }
        return null;
    }

    public void cacheEntry(PersistentEntity entity, Serializable key, Object entry) {
        if (key != null && entry != null) {
            Map<Serializable, Object> cache = firstLevelEntryCache.get(entity);
            if (cache == null) {
                cache = new ConcurrentHashMap<Serializable,Object>();
                firstLevelEntryCache.put(entity.getJavaClass(), cache);
            }
            cache.put(key, entry);
        }
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

    public void addEntityInterceptor(EntityInterceptor interceptor) {
        if (interceptor != null) {
            interceptors.add(interceptor);
        }
    }

    public void setEntityInterceptors(List<EntityInterceptor> interceptors) {
        if (interceptors!=null) this.interceptors = interceptors;
    }

    public MappingContext getMappingContext() {
        return mappingContext;
    }

    public void flush() {
        if (!exceptionOccurred) {
            boolean hasInserts = hasUpdates();
            if (hasInserts) {
                flushPendingInserts(pendingInserts);
                pendingInserts.clear();
                flushPendingUpdates(pendingUpdates);
                pendingUpdates.clear();
                executePendings(pendingDeletes);
                executePendings(postFlushOperations);
                postFlush(hasInserts);
            }
        }
        else {
            throw new InvalidDataAccessResourceUsageException("Do not flush() the Session after an exception occurs");
        }
    }

    /**
     * The default implementation of flushPendingUpdates is to iterate over each update operation and execute them one by one.
     * This may be suboptimal for stores that support batch update operations. Subclasses can override this method to implement
     * batch update more efficiently
     *
     * @param updates
     */
    protected void flushPendingUpdates(Map<PersistentEntity, Collection<PendingUpdate>> updates) {
        for (Collection<PendingUpdate> pending : updates.values()) {
            flushPendingOperations(pending);
        }
    }

    /**
     * The default implementation of flushPendingInserts is to iterate over each insert operations and execute them one by one.
     * This may be suboptimal for stores that support batch insert operations. Subclasses can override this method to implement
     * batch insert more efficiently
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
    private void flushPendingOperations(Collection inserts) {
        for (Object o : inserts) {
            PendingOperation pendingInsert = (PendingOperation) o;
            try {
                PendingOperationExecution.executePendingInsert(pendingInsert);
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
            for (Runnable pendingInsert : pendings) {
                pendingInsert.run();
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
            if (p instanceof EntityInterceptorAware) {
                ((EntityInterceptorAware)p).setEntityInterceptors(interceptors);
            }
            if (p != null)
                persisters.put(cls, p);
        }
        return p;
    }

    @SuppressWarnings("hiding")
    protected abstract Persister createPersister(Class cls, MappingContext mappingContext);

    public boolean contains(Object o) {
        if (o != null) {
            final Map<Serializable, Object> cache = firstLevelCache.get(o.getClass());
            if (cache != null && cache.containsValue(o)) {
                return true;
            }
        }
        return false;
    }

    public void clear(Object o) {
        if (o != null) {
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
    }

    public void attach(Object o) {
        if (o != null) {
            EntityPersister p = (EntityPersister) getPersister(o);

            if (p != null) {
                Serializable identifier = p.getObjectIdentifier(o);
                if (identifier != null) {
                    cacheObject(identifier, o);
                }
            }
        }
    }

    protected void cacheObject(Serializable identifier, Object o) {
        if (identifier != null && o != null) {
            Map<Serializable, Object> cache = firstLevelCache.get(o.getClass());
            if (cache == null) {
                cache = new ConcurrentHashMap<Serializable, Object>();
                firstLevelCache.put(o.getClass(), cache);
            }
            objectToKey.put(o, identifier);
            cache.put(identifier, o);
        }
    }

    public Serializable persist(Object o) {
        Assert.notNull(o, "Cannot persist null object");
        Persister persister = getPersister(o);
        if (persister != null) {
            final Serializable key = persister.persist(o);
            cacheObject(key, o);
            return key;
        }
        throw new NonPersistentTypeException("Object ["+o+"] cannot be persisted. It is not a known persistent type.");
    }

    public void refresh(Object o) {
        Assert.notNull(o, "Cannot persist null object");
        Persister persister = getPersister(o);
        if (persister != null) {
            final Serializable key = persister.refresh(o);
            cacheObject(key, o);
        }
        throw new NonPersistentTypeException("Object ["+o+"] cannot be persisted. It is not a known persistent type.");
    }

    public Object retrieve(Class type, Serializable key) {
        if (key == null || type == null) return null;
        Persister persister = getPersister(type);
        if (persister != null) {

            final PersistentEntity entity = getMappingContext().getPersistentEntity(type.getName());
            if (entity != null) {
                key = (Serializable) getMappingContext().getConversionService().convert(key, entity.getIdentity().getType());
            }
            final Map<Serializable, Object> cache = firstLevelCache.get(type);
            Object o = cache.get(key);
            if (o == null) {
                o = persister.retrieve(key);
                if (o != null)
                    cacheObject(key, o);
                return o;
            }
            return o;
        }
        throw new NonPersistentTypeException("Cannot retrieve object with key ["+key+"]. The class ["+type+"] is not a known persistent type.");
    }

    public Object proxy(Class type, Serializable key) {
        if (key == null || type == null) return null;
        Persister persister = getPersister(type);
        if (persister != null) {
            return persister.proxy(key);
        }
        throw new NonPersistentTypeException("Cannot retrieve object with key ["+key+"]. The class ["+type+"] is not a known persistent type.");
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
        if (obj != null) {
            getPendingDeletes().add(new Runnable() {
                public void run() {
                    Persister p = getPersister(obj);
                    if (p != null) {
                        p.delete(obj);
                        clear(obj);

                    }
                }
            });
        }
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
        if (objects != null) {

            final Iterator i = objects.iterator();
            if (i.hasNext()) {
                // peek at the first object to get the persister
                final Object obj = i.next();
                final Persister p = getPersister(obj);
                if (p != null) {
                    return p.persist(objects);
                }
                throw new NonPersistentTypeException("Cannot persist objects. The class ["+obj.getClass()+"] is not a known persistent type.");
            }

        }
        return Collections.emptyList();
    }

    public List retrieveAll(Class type, Iterable keys) {
        Persister p = getPersister(type);

        if (p != null) {
            return p.retrieveAll(keys);
        }
        throw new NonPersistentTypeException("Cannot retrieve objects with keys ["+keys+"]. The class ["+type+"] is not a known persistent type.");
    }

    public List retrieveAll(Class type, Serializable... keys) {
        Persister p = getPersister(type);

        if (p != null) {
            List retrieved = new ArrayList(keys.length);
            final Map<Serializable, Object> cache = firstLevelCache.get(type);
            for (int i = 0; i < keys.length; i++) {
                Serializable key = keys[i];
                Object cached = cache.get(key);
                if (cached != null) {
                    retrieved.add(i, cached);
                }
                else {
                    retrieved.add(i, retrieve(type, key));
                }
            }
            return retrieved;
        }
        throw new NonPersistentTypeException("Cannot retrieve objects with keys ["+keys+"]. The class ["+type+"] is not a known persistent type.");
    }

    public Query createQuery(Class type) {
        Persister p = getPersister(type);
        if (p!= null) {
            return p.createQuery();
        }
        throw new NonPersistentTypeException("Cannot create query. The class ["+type+"] is not a known persistent type.");
    }

    public final Transaction beginTransaction() {
        transaction = beginTransactionInternal();
        return transaction;
    }

    protected abstract Transaction beginTransactionInternal();

    public Transaction getTransaction() {
        if (transaction == null) throw new NoTransactionException("Transaction not started. Call beginTransaction() first");
        return transaction;
    }
}
