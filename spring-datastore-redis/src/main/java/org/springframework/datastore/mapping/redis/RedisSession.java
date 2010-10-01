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
package org.springframework.datastore.mapping.redis;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.datastore.mapping.core.AbstractSession;
import org.springframework.datastore.mapping.core.Datastore;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.engine.LockableEntityPersister;
import org.springframework.datastore.mapping.engine.NonPersistentTypeException;
import org.springframework.datastore.mapping.engine.Persister;
import org.springframework.datastore.mapping.model.MappingContext;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.redis.collection.RedisSet;
import org.springframework.datastore.mapping.redis.engine.RedisEntityPersister;
import org.springframework.datastore.mapping.redis.util.RedisTemplate;
import org.springframework.datastore.mapping.transactions.Transaction;
import org.springframework.transaction.CannotCreateTransactionException;

import javax.persistence.FlushModeType;
import java.io.Serializable;
import java.util.List;

/**
 * @author Graeme Rocher
 * @since 1.0
 */
public class RedisSession extends AbstractSession  {

    private RedisTemplate redisTemplate;

    public RedisSession(Datastore ds, MappingContext mappingContext, RedisTemplate template) {
        super(ds, mappingContext);

        redisTemplate = template;

    }


    @Override
    protected Persister createPersister(Class cls, MappingContext mappingContext) {
      PersistentEntity entity = mappingContext.getPersistentEntity(cls.getName());
      if(entity != null) {
          return new RedisEntityPersister(mappingContext,
                                            entity,
                                            this,
                  redisTemplate);
      }
      return null;

    }

    public boolean isConnected() {
        return true;        
    }

    @Override
    public void disconnect() {
        try {
            for (Object lockedObject : lockedObjects) {
                unlock(lockedObject);
            }
            redisTemplate.close();
        } finally {
            super.disconnect();
        }
    }

    protected Transaction beginTransactionInternal() {
        try {
            redisTemplate.multi();
        } catch (Exception e) {
            throw new CannotCreateTransactionException("Error starting Redis transaction: " + e.getMessage(), e);
        }
        return new RedisTransaction(redisTemplate);
    }

    @Override
    public void lock(Object o) {
        LockableEntityPersister ep = (LockableEntityPersister) getPersister(o);
        if(ep != null) {
            Serializable id = ep.getObjectIdentifier(o);
            if(id != null) {
                ep.lock(id);
            }
            else {
                throw new CannotAcquireLockException("Cannot lock transient instance ["+o+"]");
            }
        }
        else {
            throw new CannotAcquireLockException("Cannot lock object ["+o+"]. It is not a persistent instance!");
        }
    }

    @Override
    public void unlock(Object o) {
        if(o != null) {
            LockableEntityPersister ep = (LockableEntityPersister) getPersister(o);
            if(ep != null) {
                ep.unlock(o);
                lockedObjects.remove(o);
            }
        }
    }

    @Override
    public Object lock(Class type, Serializable key) {
        LockableEntityPersister ep = (LockableEntityPersister) getPersister(type);
        if(ep != null) {
            final Object lockedObject = ep.lock(key);
            if(lockedObject != null) {
                cacheObject(key, lockedObject);
                lockedObjects.add(lockedObject);
            }
            return lockedObject;
        }
        else {
           throw new CannotAcquireLockException("Cannot lock key ["+key+"]. It is not a persistent instance!");
        }
    }


    /**
     * Returns a random entity for the given type
     *
     * @param type The entity type
     * @return A random entity instance
     */
    public Object random(Class type) {
        flushIfNecessary();
        RedisEntityPersister ep = (RedisEntityPersister) getPersister(type);
        if(ep != null) {
            RedisSet set = (RedisSet) ep.getAllEntityIndex();
            String id = set.random();
            return retrieve(type, id);
        }
        else {
            throw new NonPersistentTypeException("The class ["+type+"] is not a known persistent type.");
        }
    }

    /**
     * Expires an instance
     *
     * @param instance The instance to expire
     * @param ttl The time to live in seconds
     */
    public void expire(Object instance, int ttl) {
        if(instance != null) {
            final RedisEntityPersister ep = (RedisEntityPersister) getPersister(instance);
            if(ep != null) {
                final Serializable key = ep.getObjectIdentifier(instance);
                if(key != null) {
                    expire(instance.getClass(), key, ttl);
                }
            }
            else {
              throw new NonPersistentTypeException("The class ["+instance.getClass()+"] is not a known persistent type.");  
            }
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
        final RedisEntityPersister ep = (RedisEntityPersister) getPersister(type);

        if(ep != null) {
            String entityKey = ep.getRedisKey(key);
            redisTemplate.expire(entityKey, ttl);
            new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(ttl*1000);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                    final RedisSession newSession = (RedisSession) getDatastore().connect();
                    RedisEntityPersister newEp = (RedisEntityPersister) newSession.getPersister(type);
                    newEp.getAllEntityIndex().remove(key);
                }
            }).start();
        }
        else {
          throw new NonPersistentTypeException("The class ["+type+"] is not a known persistent type.");
        }
    }

    private void flushIfNecessary() {
        if(getFlushMode() == FlushModeType.AUTO) flush();
    }

    @Override
    protected void postFlush(boolean hasUpdates) {
        if(hasUpdates) {
            final List<String> keys = redisTemplate.keys("~*");
            if(keys != null && !keys.isEmpty())
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
        RedisEntityPersister ep = (RedisEntityPersister) getPersister(type);
        if(ep != null) {
            RedisSet set = (RedisSet) ep.getAllEntityIndex();
            String id = set.pop();
            Object result = null;
            try {
                result = retrieve(type, id);
                return result;
            } finally {
                if(result != null)
                    delete(result);
            }
        }
        else {
            throw new NonPersistentTypeException("The class ["+type+"] is not a known persistent type.");
        }
    }

    public RedisTemplate getNativeInterface() {
        return redisTemplate;
    }


}
