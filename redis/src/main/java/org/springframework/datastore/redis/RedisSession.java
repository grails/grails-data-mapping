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
package org.springframework.datastore.redis;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.datastore.core.AbstractSession;
import org.springframework.datastore.core.Datastore;
import org.springframework.datastore.engine.LockableEntityPersister;
import org.springframework.datastore.engine.NonPersistentTypeException;
import org.springframework.datastore.engine.Persister;
import org.springframework.datastore.mapping.MappingContext;
import org.springframework.datastore.mapping.PersistentEntity;
import org.springframework.datastore.redis.collection.RedisSet;
import org.springframework.datastore.redis.engine.RedisEntityPersister;
import org.springframework.datastore.redis.util.RedisTemplate;
import org.springframework.datastore.transactions.Transaction;
import org.springframework.transaction.CannotCreateTransactionException;
import sma.RedisClient;

import java.io.Serializable;
import java.util.*;

/**
 * @author Graeme Rocher
 * @since 1.0
 */
public class RedisSession extends AbstractSession<RedisClient>  {

    public static final String CONFIG_HOST = "host";
    public static final String CONFIG_PORT = "port";
    public static final String LOCALHOST = "localhost";

    private RedisTemplate redisClient;
    public static final String CONFIG_PASSWORD = "password";

    public RedisSession(Datastore ds, Map<String, String> connectionInfo, MappingContext mappingContext) {
        super(ds, connectionInfo != null ? connectionInfo : Collections.<String, String>emptyMap(), mappingContext);
        String host = LOCALHOST;

        if(this.connectionDetails.containsKey(CONFIG_HOST)) {
            host = connectionDetails.get(CONFIG_HOST);
        }
        int port = RedisClient.DEFAULT_PORT;
        if(connectionDetails.containsKey(CONFIG_PORT)) {
            try {
                port = Integer.parseInt(connectionDetails.get(CONFIG_PORT));
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        final RedisClient client = new RedisClient(host, port);


        redisClient = new RedisTemplate(client);
        if(connectionDetails.containsKey(CONFIG_PASSWORD)) {
            redisClient.setPassword(connectionDetails.get(CONFIG_PASSWORD));
        }
    }

    @Override
    protected Persister createPersister(Class cls, MappingContext mappingContext) {
      PersistentEntity entity = mappingContext.getPersistentEntity(cls.getName());
      if(entity != null) {
          return new RedisEntityPersister(mappingContext,
                                            entity,
                                            this,
                                            redisClient);
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
            redisClient.close();
        } finally {
            super.disconnect();
        }
    }

    protected Transaction beginTransactionInternal() {
        try {
            redisClient.multi();
        } catch (Exception e) {
            throw new CannotCreateTransactionException("Error starting Redis transaction: " + e.getMessage(), e);
        }
        return new RedisTransaction(redisClient);
    }

    @Override
    public void lock(Object o) {
        LockableEntityPersister ep = (LockableEntityPersister) getPersister(o);
        if(ep != null) {
            Serializable id = ep.getObjectIdentifier(o);
            if(id != null) {
                ep.lock(id);
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
            lockedObjects.add(lockedObject);
            return lockedObject;
        }
        else {
           throw new CannotAcquireLockException("Cannot lock key ["+key+"]. It is not a persistent instance!");
        }
    }

    public Object random(Class type) {
        RedisEntityPersister ep = (RedisEntityPersister) getPersister(type);
        if(ep != null) {
            RedisSet set = (RedisSet) ep.getAllEntityIndex();
            String id = set.random();
            return ep.retrieve(id);
        }
        else {
            throw new NonPersistentTypeException("The class ["+type+"] is not a known persistent type.");
        }
    }

    public Object pop(Class type) {
        RedisEntityPersister ep = (RedisEntityPersister) getPersister(type);
        if(ep != null) {
            RedisSet set = (RedisSet) ep.getAllEntityIndex();
            String id = set.pop();
            Object result = null;
            try {
                result = ep.retrieve(id);
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

    public RedisClient getNativeInterface() {
        return redisClient.getRedisClient();
    }


}
