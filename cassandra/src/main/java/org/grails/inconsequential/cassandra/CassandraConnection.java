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
package org.grails.inconsequential.cassandra;

import me.prettyprint.cassandra.service.CassandraClient;
import me.prettyprint.cassandra.service.CassandraClientPool;
import org.grails.inconsequential.cassandra.engine.CassandraEntityPersister;
import org.grails.inconsequential.core.AbstractDatastore;
import org.grails.inconsequential.core.AbstractObjectDatastoreConnection;
import org.grails.inconsequential.core.Key;
import org.grails.inconsequential.engine.Persister;
import org.grails.inconsequential.kv.mapping.KeyValueMappingContext;
import org.grails.inconsequential.mapping.MappingContext;
import org.grails.inconsequential.mapping.PersistentEntity;
import org.grails.inconsequential.tx.Transaction;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.TransactionSystemException;

import java.util.Map;
import java.util.UUID;

/**
 * @author Graeme Rocher
 * @since 1.0
 */
public class CassandraConnection extends AbstractObjectDatastoreConnection {
    private CassandraClient cassandraClient;
    private CassandraClientPool connectionPool;

    public CassandraConnection(Map<String, String> connectionDetails, MappingContext context, CassandraClientPool connectionPool, CassandraClient client) {
        super(connectionDetails, context);
        this.connectionPool = connectionPool;
        this.cassandraClient = client;
    }

    @Override
    protected Persister createPersister(Class cls, MappingContext mappingContext) {
      PersistentEntity entity = mappingContext.getPersistentEntity(cls.getName());
      if(entity != null) {
          return new CassandraEntityPersister(entity,this, cassandraClient);
      }
      return null;
    }

    public boolean isConnected() {
        return !cassandraClient.isReleased();
    }

    @Override
    public void disconnect() {
        try {
            connectionPool.releaseClient(cassandraClient);
        } catch (Exception e) {
            throw new DataAccessResourceFailureException("Failed to release Cassandra client connection: " + e.getMessage(), e);
        } finally {
            super.disconnect();
        }
    }

    public Transaction beginTransaction() {
        throw new TransactionSystemException("Transactions are not supported by Cassandra");
    }

    public Key createKey(Object nativeKey) {
        return new CassandraKey(nativeKey);
    }
}
