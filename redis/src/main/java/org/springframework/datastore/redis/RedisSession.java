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
import org.springframework.datastore.redis.util.JedisTemplate;
import org.springframework.datastore.redis.util.RedisClientTemplate;
import org.springframework.datastore.redis.util.RedisTemplate;
import org.springframework.datastore.transactions.Transaction;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.util.ClassUtils;

import java.io.Serializable;
import java.util.*;

import static org.springframework.datastore.config.utils.ConfigUtils.*;

/**
 * @author Graeme Rocher
 * @since 1.0
 */
public class RedisSession extends AbstractSession  {

    private static final boolean redisClientAvailable =
            ClassUtils.isPresent("sma.RedisClient", RedisSession.class.getClassLoader());

    private static final boolean jedisClientAvailable =
            ClassUtils.isPresent("redis.clients.jedis.Jedis", RedisSession.class.getClassLoader());

    public static final String CONFIG_HOST = "host";
    public static final String CONFIG_TIMEOUT = "timeout";
    public static final String CONFIG_PORT = "port";
    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 6379;

    private RedisTemplate redisTemplate;
    public static final String CONFIG_PASSWORD = "password";

    public RedisSession(Datastore ds, Map<String, String> connectionInfo, MappingContext mappingContext) {
        super(ds, connectionInfo != null ? connectionInfo : Collections.<String, String>emptyMap(), mappingContext);
        if(redisClientAvailable) {
           redisTemplate = RedisClientTemplateFactory.create(connectionDetails);
        }
        else if(jedisClientAvailable) {
           redisTemplate = JedisTemplateFactory.create(connectionDetails);
        }
        else{
            throw new IllegalStateException("Cannot create RedisSession. Neither jedis nor java-redis-client is found on the classpath");
        }
    }


    static class JedisTemplateFactory {
        static RedisTemplate create(Map connectionDetails) {
            String host = read(String.class, CONFIG_HOST, connectionDetails, DEFAULT_HOST);
            int port = read(Integer.class, CONFIG_PORT, connectionDetails, DEFAULT_PORT);
            int timeout = read(Integer.class, CONFIG_TIMEOUT, connectionDetails, 2000);

            final JedisTemplate template = new JedisTemplate(host, port, timeout);
            String password = read(String.class, CONFIG_PASSWORD, connectionDetails, null);
            if(password != null) {
                template.setPassword(password);
            }

            return template;
        }
    }

    static class RedisClientTemplateFactory {
        static RedisTemplate create(Map connectionDetails) {
            String host = read(String.class, CONFIG_HOST, connectionDetails, DEFAULT_HOST);
            int port = read(Integer.class, CONFIG_PORT, connectionDetails, DEFAULT_PORT);
            String password = read(String.class, CONFIG_PASSWORD, connectionDetails, null);
            final RedisClientTemplate template = new RedisClientTemplate(host, port);
            if(password != null) {
                template.setPassword(password);
            }

            return template;
        }
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

    public RedisTemplate getNativeInterface() {
        return redisTemplate;
    }


}
