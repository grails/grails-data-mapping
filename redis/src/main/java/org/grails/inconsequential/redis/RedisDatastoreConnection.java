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

import org.grails.inconsequential.core.*;
import org.grails.inconsequential.engine.Persister;
import org.grails.inconsequential.mapping.MappingContext;
import org.grails.inconsequential.mapping.PersistentEntity;
import org.grails.inconsequential.redis.engine.RedisEntityPersister;
import org.grails.inconsequential.tx.Transaction;
import org.jredis.JRedis;
import org.jredis.RedisException;
import org.jredis.connector.ConnectionSpec;
import org.jredis.ri.alphazero.JRedisClient;
import org.jredis.ri.alphazero.JRedisService;
import org.jredis.ri.alphazero.connection.DefaultConnectionSpec;
import org.springframework.transaction.CannotCreateTransactionException;

import java.util.Map;

/**
 * @author Graeme Rocher
 * @since 1.0
 */
public class RedisDatastoreConnection extends AbstractObjectDatastoreConnection<String> {

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
          return new RedisEntityPersister(entity, jredisClient);
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


}
