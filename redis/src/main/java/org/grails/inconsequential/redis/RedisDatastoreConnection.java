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
package org.grails.inconsequential.redis;

import org.grails.inconsequential.core.AbstractObjectDatastoreConnection;
import org.grails.inconsequential.core.Key;
import org.grails.inconsequential.engine.Persister;
import org.grails.inconsequential.mapping.MappingContext;
import org.grails.inconsequential.mapping.PersistentEntity;
import org.grails.inconsequential.redis.engine.RedisEntityPersister;
import org.grails.inconsequential.tx.Transaction;
import org.jredis.JRedis;
import org.jredis.RedisException;
import org.jredis.connector.ConnectionSpec;
import org.jredis.ri.alphazero.JRedisService;
import org.jredis.ri.alphazero.connection.DefaultConnectionSpec;
import org.springframework.transaction.CannotCreateTransactionException;

import java.io.Serializable;
import java.util.*;

/**
 * @author Graeme Rocher
 * @since 1.0
 */
public class RedisDatastoreConnection extends AbstractObjectDatastoreConnection<Long> implements Map {

    private JRedis jredisClient;

    public RedisDatastoreConnection(Map<String, String> connectionDetails, MappingContext mappingContext) {
        super(connectionDetails, mappingContext);
        int timeout = 30000; // msecs
        ConnectionSpec connSpec = DefaultConnectionSpec.newSpec();
        connSpec.setSocketProperty(ConnectionSpec.SocketProperty.SO_TIMEOUT, timeout);
        jredisClient = new JRedisService(connSpec,JRedisService.default_connection_count);
    }

    public JRedis getJredisClient() {
        return jredisClient;
    }

    @Override
    protected Persister createPersister(Class cls, MappingContext mappingContext) {
      PersistentEntity entity = mappingContext.getPersistentEntity(cls.getName());
      if(entity != null) {
          return new RedisEntityPersister(entity,this, jredisClient);
      }
      return null;

    }

    public boolean isConnected() {
        return true;        
    }

    @Override
    public void disconnect() {
        try {
            jredisClient.quit();
        } finally {
            super.disconnect();
        }
    }

    public Transaction beginTransaction() {
        try {
            jredisClient.multi();
            return new RedisTransaction(jredisClient);
        } catch (RedisException e) {
            throw new CannotCreateTransactionException("Failed to create Redis transaction: " + e.getMessage(),e );
        }

    }

    public int size() {
        try {
            return ((Long)jredisClient.dbsize()).intValue();
        } catch (RedisException e) {
            return 0;
        }
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public boolean containsKey(Object o) {
        if(o!=null) {
            try {
                jredisClient.exists(o.toString());
            } catch (RedisException e) {
                return false;
            }
        }
        return false;
    }

    public boolean containsValue(Object o) {
        throw new UnsupportedOperationException("Method containsValue(Object) is not supported");
    }

    public Object get(Object key) {
        if(key!=null) {
            try {
                final byte[] bytes = jredisClient.get(key.toString());
                if(bytes != null)
                    return new String(bytes);
                return null;
            } catch (RedisException e) {
                return null;
            }
        }
        return null;
    }

    public Object put(Object key, Object value) {
        if(key != null) {
            byte [] oldValue;
            try {
                oldValue = jredisClient.getset(key.toString(), value.toString());
                if(oldValue != null)
                    return new String(oldValue);
                return null;
            } catch (RedisException e) {
                return null;
            }
        }
        return null;
    }

    public Object remove(Object key) {
        if(key != null) {
            try {
                return jredisClient.del(key.toString());

            } catch (RedisException e) {
                return null;
            }
        }
        return null;
    }

    public void putAll(Map map) {
        Map<String, byte[]> putMap = new HashMap<String, byte[]>();
        for (Object key : map.keySet()) {
            final Object val = map.get(key);
            if(val != null)
                putMap.put(key.toString(), val.toString().getBytes());
        }
        try {
            jredisClient.mset(putMap);
        } catch (RedisException e) {
            // do nothing
        }
    }

    public void clear() {
        try {
            jredisClient.flushdb();
        } catch (RedisException e) {
            // do nothing
        }
    }

    public Set keySet() {
        final List<String> keys;
        try {
            keys = jredisClient.keys("*");
            return new HashSet(keys);
        } catch (RedisException e) {
            return Collections.emptySet();
        }
    }

    public Collection values() {
        throw new UnsupportedOperationException("Method values() is not supported");
    }

    public Set entrySet() {
        throw new UnsupportedOperationException("Method entrySet() is not supported");
    }

    public Key<Long> createKey(Long nativeKey) {
        return new RedisKey(nativeKey);
    }
}
