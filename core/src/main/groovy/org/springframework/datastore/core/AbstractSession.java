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
package org.springframework.datastore.core;

import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.datastore.engine.*;
import org.springframework.datastore.mapping.MappingContext;
import org.springframework.datastore.mapping.PersistentEntity;
import org.springframework.datastore.query.Query;
import org.springframework.datastore.transactions.Transaction;
import org.springframework.transaction.NoTransactionException;

import javax.persistence.FlushModeType;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Abstract implementation of the ObjectDatastore interface that uses
 * a list of {@link org.springframework.datastore.engine.Persister} instances
 * to save, update and delete instances
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public abstract class AbstractSession<N> implements Session, SessionImplementor {
    protected Map<Class,Persister> persisters = new ConcurrentHashMap<Class,Persister>();
    private MappingContext mappingContext;
    protected List<EntityInterceptor> interceptors = new ArrayList<EntityInterceptor>();
    protected ConcurrentLinkedQueue lockedObjects = new ConcurrentLinkedQueue();
    private Transaction transaction;
    private Datastore datastore;
    private FlushModeType flushMode = FlushModeType.AUTO;
    protected Map<Class, Map<Serializable, Object>> firstLevelCache = new ConcurrentHashMap<Class, Map<Serializable, Object>>();
    protected Map<Object, Serializable> objectToKey = new ConcurrentHashMap<Object, Serializable>();
    protected Collection<Runnable> pendingInserts = new ConcurrentLinkedQueue<Runnable>();
    protected Collection<Runnable> pendingUpdates = new ConcurrentLinkedQueue<Runnable>();
    protected Collection<Runnable> pendingDeletes = new ConcurrentLinkedQueue<Runnable>();
    private boolean exceptionOccurred;

    public AbstractSession(Datastore datastore,MappingContext mappingContext) {
        super();
        this.mappingContext = mappingContext;
        this.datastore = datastore;
    }

    public Collection<Runnable> getPendingInserts() {
        return pendingInserts;
    }

    public Collection<Runnable> getPendingUpdates() {
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
        if(interceptor != null) {
            this.interceptors.add(interceptor);
        }
    }

    public void setEntityInterceptors(List<EntityInterceptor> interceptors) {
        if(interceptors!=null) this.interceptors = interceptors;
    }

    public MappingContext getMappingContext() {
        return this.mappingContext;
    }

    public void flush() {
        if(!exceptionOccurred) {
            executePendings(pendingInserts);
            executePendings(pendingUpdates);
            executePendings(pendingDeletes);
        }
        else {
            throw new InvalidDataAccessResourceUsageException("Do not flush() the Session after an exception occurs");
        }
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
        pendingInserts.clear();
        pendingUpdates.clear();
        pendingDeletes.clear();
        exceptionOccurred = false;
    }

    public final Persister getPersister(Object o) {
        if(o == null) return null;
        Class cls;
        if(o instanceof Class) {
           cls = (Class) o;
        }
        else if(o instanceof PersistentEntity) {
            cls = ((PersistentEntity)o).getJavaClass();
        }
        else {
           cls = o.getClass();
        }
        Persister p = this.persisters.get(cls);
        if(p == null) {
            p = createPersister(cls, getMappingContext());
            firstLevelCache.put(cls, new ConcurrentHashMap<Serializable, Object>());
            if(p instanceof EntityInterceptorAware) {
                ((EntityInterceptorAware)p).setEntityInterceptors(interceptors);
            }
            if(p != null)
                this.persisters.put(cls, p);
        }
        return p;
    }

    protected abstract Persister createPersister(Class cls, MappingContext mappingContext);

    public boolean contains(Object o) {
        if(o != null) {
            final Map<Serializable, Object> cache = firstLevelCache.get(o.getClass());
            if(cache != null && cache.containsValue(o)) {
                return true;
            }
        }
        return false;
    }

    public void clear(Object o) {
        if(o != null) {
            final Map<Serializable, Object> cache = firstLevelCache.get(o.getClass());
            if(cache != null) {

                Serializable key = objectToKey.get(o);
                if(key != null) {
                    cache.remove(key);
                    objectToKey.remove(o);
                }

            }
        }
    }

    public void attach(Object o) {
        if(o != null) {
            EntityPersister p = (EntityPersister) getPersister(o);

            if(p != null) {
                Serializable identifier = p.getObjectIdentifier(o);
                if(identifier != null) {
                    cacheObject(identifier, o);
                }
            }
        }
    }

    protected void cacheObject(Serializable identifier, Object o) {
        if(identifier != null && o != null) {
            Map<Serializable, Object> cache = firstLevelCache.get(o.getClass());
            if(cache == null) {
                cache = new ConcurrentHashMap<Serializable, Object>();
                firstLevelCache.put(o.getClass(), cache);
            }
            objectToKey.put(o, identifier);
            cache.put(identifier, o);
        }
    }

    public Serializable persist(Object o) {
        if(o == null) throw new IllegalArgumentException("Cannot persist null object");
        Persister persister = getPersister(o);
        if(persister != null) {
            final Serializable key = persister.persist(o);
            cacheObject(key, o);
            return key;
        }
        throw new NonPersistentTypeException("Object ["+o+"] cannot be persisted. It is not a known persistent type.");
    }

    public Object retrieve(Class type, Serializable key) {
        if(key == null || type == null) return null;
        Persister persister = getPersister(type);
        final Map<Serializable, Object> cache = firstLevelCache.get(type);
        Object o = cache.get(key);
        if(o == null) {

            if(persister != null) {
                o = persister.retrieve(key);
                if(o != null)
                    cacheObject(key, o);
                return o;
            }
            throw new NonPersistentTypeException("Cannot retrieve object with key ["+key+"]. The class ["+type+"] is not a known persistent type.");
        }
        return o;
    }

    public Object proxy(Class type, Serializable key) {
        if(key == null || type == null) return null;
        Persister persister = getPersister(type);
        if(persister != null) {
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
        if(o != null)
            lockedObjects.remove(o);
    }

    public void delete(final Object obj) {
        if(obj != null) {
            pendingDeletes.add(new Runnable() {
                public void run() {
                    Persister p = getPersister(obj);
                    if(p != null) {
                        p.delete(obj);
                        clear(obj);

                    }
                }
            });

        }
    }

    public void delete(final Iterable objects) {
        if(objects != null) {
            for (Object object : objects) {
                if(object != null) {
                    final Persister p = getPersister(object);
                    if(p != null) {
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
            }
        }
    }

    /**
     * Performs clear up. Subclasses should always call into this super
     * implementation.
     */
    public void disconnect() {
        AbstractDatastore.clearCurrentConnection();
    }

    public List<Serializable> persist(Iterable objects) {
        if(objects != null) {

            final Iterator i = objects.iterator();
            if(i.hasNext()) {
                // peek at the first object to get the persister
                final Object obj = i.next();
                final Persister p = getPersister(obj);
                if(p != null) {
                    return p.persist(objects);
                }
                else {
                    throw new NonPersistentTypeException("Cannot persist objects. The class ["+obj.getClass()+"] is not a known persistent type.");
                }
            }

        }
        return Collections.emptyList();
    }

    public List retrieveAll(Class type, Iterable keys) {
        Persister p = getPersister(type);

        if(p != null) {
            return p.retrieveAll(keys);
        }
        throw new NonPersistentTypeException("Cannot retrieve objects with keys ["+keys+"]. The class ["+type+"] is not a known persistent type.");
    }

    public List retrieveAll(Class type, Serializable... keys) {
        Persister p = getPersister(type);

        if(p != null) {
            List retrieved = new ArrayList(keys.length);
            final Map<Serializable, Object> cache = firstLevelCache.get(type);
            for (int i = 0; i < keys.length; i++) {
                Serializable key = keys[i];
                Object cached = cache.get(key);
                if(cached != null) {
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
        if(p!= null) {
            return p.createQuery();
        }
        throw new NonPersistentTypeException("Cannot create query. The class ["+type+"] is not a known persistent type.");
    }

    public final Transaction beginTransaction() {
        this.transaction = beginTransactionInternal();
        return this.transaction;
    }

    protected abstract Transaction beginTransactionInternal();

    public Transaction getTransaction() {
        if(transaction == null) throw new NoTransactionException("Transaction not started. Call beginTransaction() first");
        return this.transaction;
    }
}
