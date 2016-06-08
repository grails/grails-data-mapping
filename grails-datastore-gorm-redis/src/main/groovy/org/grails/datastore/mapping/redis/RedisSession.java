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
package org.grails.datastore.mapping.redis;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.FlushModeType;

import org.grails.datastore.mapping.engine.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.CannotAcquireLockException;
import org.grails.datastore.mapping.core.AbstractSession;
import org.grails.datastore.mapping.core.Datastore;
import org.grails.datastore.mapping.core.impl.PendingInsert;
import org.grails.datastore.mapping.core.impl.PendingOperation;
import org.grails.datastore.mapping.core.impl.PendingUpdate;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.redis.collection.RedisSet;
import org.grails.datastore.mapping.redis.engine.RedisEntityPersister;
import org.grails.datastore.mapping.redis.util.RedisCallback;
import org.grails.datastore.mapping.redis.util.RedisTemplate;
import org.grails.datastore.mapping.transactions.Transaction;
import org.springframework.transaction.CannotCreateTransactionException;

/**
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class RedisSession extends AbstractSession<RedisTemplate> {

    private RedisTemplate redisTemplate;

    public RedisSession(Datastore ds, MappingContext mappingContext, RedisTemplate template,
            ApplicationEventPublisher publisher) {
        super(ds, mappingContext, publisher);
        redisTemplate = template;
    }


    @Override
    protected void flushPendingInserts(final Map<PersistentEntity, Collection<PendingInsert>> inserts) {
        // Optimizes saving multiple entities at once
        for (final PersistentEntity entity : inserts.keySet()) {
            final Collection<PendingInsert> pendingInserts = inserts.get(entity);

            final List<PendingOperation<RedisEntry, Long>> postOperations = new LinkedList<PendingOperation<RedisEntry, Long>>();

            final RedisEntityPersister persister = (RedisEntityPersister)getPersister(entity);

            redisTemplate.pipeline(new RedisCallback<RedisTemplate>() {
                public Object doInRedis(RedisTemplate redis) throws IOException {
                    for (PendingInsert<RedisEntry, Long> pendingInsert : pendingInserts) {
                        final EntityAccess entityAccess = pendingInsert.getEntityAccess();
                        if (persister.cancelInsert(entity, entityAccess)) {
                            continue;
                        }

                        List<PendingOperation<RedisEntry, Long>> preOperations = pendingInsert.getPreOperations();
                        for (PendingOperation<RedisEntry, Long> preOperation : preOperations) {
                            preOperation.run();
                        }

                        persister.storeEntry(entity, entityAccess, pendingInsert.getNativeKey(),
                                             pendingInsert.getNativeEntry());
                        persister.firePostInsertEvent(entity, entityAccess);
                        postOperations.addAll(pendingInsert.getCascadeOperations());
                    }
                    for (PendingOperation<RedisEntry, Long> pendingOperation : postOperations) {
                        pendingOperation.run();
                    }
                    return null;
                }
            });
        }
    }

    @Override
    protected void flushPendingUpdates(Map<PersistentEntity, Collection<PendingUpdate>> updates) {
        // Optimizes saving multiple entities at once
        for (final PersistentEntity entity : updates.keySet()) {
            final Collection<PendingUpdate> pendingInserts = updates.get(entity);

            final List<PendingOperation<RedisEntry, Long>> postOperations = new LinkedList<PendingOperation<RedisEntry, Long>>();

            final RedisEntityPersister persister = (RedisEntityPersister)getPersister(entity);

            redisTemplate.pipeline(new RedisCallback<RedisTemplate>() {
                public Object doInRedis(RedisTemplate redis) throws IOException {
                    for (PendingUpdate<RedisEntry, Long> pendingInsert : pendingInserts) {
                        final EntityAccess entityAccess = pendingInsert.getEntityAccess();
                        if (persister.cancelUpdate(entity, entityAccess)) {
                            continue;
                        }

                        List<PendingOperation<RedisEntry, Long>> preOperations = pendingInsert.getPreOperations();
                        for (PendingOperation<RedisEntry, Long> preOperation : preOperations) {
                            preOperation.run();
                        }

                        persister.updateEntry(entity, entityAccess, pendingInsert.getNativeKey(),
                                              pendingInsert.getNativeEntry());
                        persister.firePostUpdateEvent(entity, entityAccess);
                        postOperations.addAll(pendingInsert.getCascadeOperations());
                    }
                    for (PendingOperation<RedisEntry, Long> pendingOperation : postOperations) {
                        pendingOperation.run();
                    }
                    return null;
                }
            });
        }
    }

    @Override
    protected Persister createPersister(Class cls, MappingContext mappingContext) {
        PersistentEntity entity = mappingContext.getPersistentEntity(cls.getName());
        if (entity == null) {
            return null;
        }

        return new RedisEntityPersister(mappingContext, entity, this, redisTemplate, publisher);
    }

    @Override
    public void disconnect() {
        try {
            for (Object lockedObject : lockedObjects) {
                unlock(lockedObject);
            }
        }
        finally {
            super.disconnect();
        }
    }

    @Override
    protected Transaction<RedisTemplate> beginTransactionInternal() {
        try {
            redisTemplate.multi();
        }
        catch (Exception e) {
            throw new CannotCreateTransactionException("Error starting Redis transaction: " + e.getMessage(), e);
        }
        return new RedisTransaction(redisTemplate);
    }

    @Override
    public void lock(Object o) {
        LockableEntityPersister ep = (LockableEntityPersister)getPersister(o);
        if (ep == null) {
            throw new CannotAcquireLockException("Cannot lock object [" + o + "]. It is not a persistent instance!");
        }

        Serializable id = ep.getObjectIdentifier(o);
        if (id == null) {
            throw new CannotAcquireLockException("Cannot lock transient instance [" + o + "]");
        }

        ep.lock(id);
    }

    @Override
    public void unlock(Object o) {
        if (o == null) {
            return;
        }

        LockableEntityPersister ep = (LockableEntityPersister)getPersister(o);
        if (ep == null) {
            return;
        }

        ep.unlock(o);
        lockedObjects.remove(o);
    }

    @Override
    public Object lock(Class type, Serializable key) {
        LockableEntityPersister ep = (LockableEntityPersister)getPersister(type);
        if (ep == null) {
            throw new CannotAcquireLockException("Cannot lock key [" + key + "]. It is not a persistent instance!");
        }

        final Object lockedObject = ep.lock(key);
        if (lockedObject != null) {
            cacheObject(key, lockedObject);
            lockedObjects.add(lockedObject);
        }
        return lockedObject;
    }

    /**
     * Returns a random entity for the given type
     *
     * @param type The entity type
     * @return A random entity instance
     */
    public Object random(Class type) {
        flushIfNecessary();
        RedisEntityPersister ep = (RedisEntityPersister)getPersister(type);
        if (ep == null) {
            throw new NonPersistentTypeException("The class [" + type.getName() + "] is not a known persistent type.");
        }

        RedisSet set = (RedisSet)ep.getAllEntityIndex();
        String id = set.random();
        return retrieve(type, id);
    }

    /**
     * Expires an instance
     *
     * @param instance The instance to expire
     * @param ttl The time to live in seconds
     */
    public void expire(Object instance, int ttl) {
        if (instance == null) {
            return;
        }

        final RedisEntityPersister ep = (RedisEntityPersister)getPersister(instance);
        if (ep == null) {
            throw new NonPersistentTypeException("The class [" + instance.getClass().getName()
                    + "] is not a known persistent type.");
        }

        final Serializable key = ep.getObjectIdentifier(instance);
        if (key != null) {
            expire(instance.getClass(), key, ttl);
        }
    }

    /**
     * Expires an entity for the given type, identifier and time to live
     *
     * @param type The entity type
     * @param key The entity key
     * @param ttl The time to live in seconds
     */
    public void expire(final Class type, final Serializable key, final int ttl) {
        final RedisEntityPersister ep = (RedisEntityPersister)getPersister(type);

        if (ep == null) {
            throw new NonPersistentTypeException("The class [" + type + "] is not a known persistent type.");
        }

        String entityKey = ep.getRedisKey(key);
        redisTemplate.expire(entityKey, ttl);
        new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(ttl * 1000);
                }
                catch (InterruptedException e) {
                    // ignore
                }
                final RedisSession newSession = (RedisSession)getDatastore().connect();
                RedisEntityPersister newEp = (RedisEntityPersister)newSession.getPersister(type);
                newEp.getAllEntityIndex().remove(key);
            }
        }).start();
    }

    private void flushIfNecessary() {
        if (getFlushMode() == FlushModeType.AUTO) {
            flush();
        }
    }

    @Override
    protected void postFlush(boolean hasUpdates) {
        if (!hasUpdates) {
            return;
        }

        final Set<String> keys = redisTemplate.keys("~*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.del(keys.toArray(new String[keys.size()]));
        }
    }

    /**
     * Locates a random entity and removes it within the same operation
     *
     * @param type The entity type
     * @return A random entity
     */
    public Object pop(Class type) {
        flushIfNecessary();
        RedisEntityPersister ep = (RedisEntityPersister)getPersister(type);
        if (ep == null) {
            throw new NonPersistentTypeException("The class [" + type + "] is not a known persistent type.");
        }

        RedisSet set = (RedisSet)ep.getAllEntityIndex();
        String id = set.pop();
        Object result = null;
        try {
            result = retrieve(type, id);
            return result;
        }
        finally {
            if (result != null) {
                delete(result);
            }
        }
    }

    public RedisTemplate getNativeInterface() {
        return redisTemplate;
    }
}
