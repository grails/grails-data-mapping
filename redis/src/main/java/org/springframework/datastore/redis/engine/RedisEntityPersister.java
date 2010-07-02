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
package org.springframework.datastore.redis.engine;

import org.springframework.datastore.engine.Indexer;
import org.springframework.datastore.keyvalue.engine.AbstractKeyValueEntityPesister;
import org.springframework.datastore.mapping.PersistentEntity;
import org.springframework.datastore.mapping.types.Association;
import org.springframework.datastore.redis.RedisDatastoreConnection;
import org.springframework.datastore.redis.RedisEntry;
import org.jredis.JRedis;
import org.jredis.RedisException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataRetrievalFailureException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * An {@link org.springframework.datastore.engine.EntityPersister} for the Redis NoSQL datastore
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class RedisEntityPersister extends AbstractKeyValueEntityPesister<RedisEntry, Long> {
    private JRedis jredisClient;
    private RedisAssociationIndexer indexer;

    public RedisEntityPersister(PersistentEntity entity, RedisDatastoreConnection conn, final JRedis jredisClient) {
        super(entity, conn);
        this.jredisClient = jredisClient;
    }


    private Long getLong(Object key) {
        return typeConverter.convertIfNecessary(key, Long.class);
    }

    @Override
    protected RedisEntry createNewEntry(String family) {
        return new RedisEntry(family);
    }

    @Override
    protected Object getEntryValue(RedisEntry nativeEntry, String property) {
        return nativeEntry.get(property);
    }

    @Override
    protected void setEntryValue(RedisEntry nativeEntry, String key, Object value) {
        if(!(value instanceof byte[])) {
            value = value.toString().getBytes();
        }
        nativeEntry.put(key, value);
    }

    @Override
    protected RedisEntry retrieveEntry(PersistentEntity persistentEntity, String family, Serializable key) {
        String hashKey = family + ":" + getLong(key);

        List<String> props = persistentEntity.getPersistentPropertyNames();

        try {
            final List<byte[]> values = jredisClient.hmget(hashKey, props.toArray(new String[props.size()]));
            if(entityDoesntExistForValues(values)) return null;
            RedisEntry entry = new RedisEntry(family);
            for (int i = 0; i < props.size(); i++) {
                  entry.put(props.get(i), values.get(i));
            }
            return entry;
        } catch (RedisException e) {
            throw new DataRetrievalFailureException("Unable to read entry for key ["+hashKey+"]", e);
        }
    }

    private boolean entityDoesntExistForValues(List<byte[]> values) {
        if(values == null) return true;
        return values.size() == 0 || (values.size() == 1 && values.get(0) == null);
    }


    @Override
    protected void updateEntry(PersistentEntity persistentEntity, Long key, RedisEntry nativeEntry) {
        String family = getFamily(persistentEntity, persistentEntity.getMapping());
        performInsertion(family, key, nativeEntry);
    }

    @Override
    protected Long storeEntry(PersistentEntity persistentEntity, RedisEntry nativeEntry) {
        final String family = nativeEntry.getFamily();
        Long id = generateIdentifier(family);
        return performInsertion(family, id, nativeEntry);
    }

    private Long performInsertion(String family, Long id, RedisEntry nativeEntry) {
        try {
            String key = family + ":" + id;

            jredisClient.hmset(key,nativeEntry);
            return id;
        } catch (RedisException e) {
            throw new DataAccessResourceFailureException("Exception occurred persisting entry ["+family+"]: " + e.getMessage(),e);
        }
        catch (Exception e) {
            throw new DataAccessResourceFailureException("Exception occurred persisting entry ["+family+"]: " + e.getMessage(),e);
        }
    }

    protected Long generateIdentifier(String family) {
        Long id = null;
        try {
            id = jredisClient.incr(family + ".next_id");
        } catch (RedisException e) {
            throw new DataAccessResourceFailureException("Exception occured generating identifier for entity ["+family+"]",e );
        }
        return id;
    }

    @Override
    protected void deleteEntries(String family, List<Long> keys) {
        List<String> actualKeys = new ArrayList<String>();
        for (Long key : keys) {
            actualKeys.add(family + ":" + key);
        }
        try {
            jredisClient.del(actualKeys.toArray(new String[actualKeys.size()]));
        } catch (RedisException e) {
            throw new DataAccessResourceFailureException("Exception deleting persistent entries ["+actualKeys+"]: " + e.getMessage(),e);
        }
    }

    @Override
    protected Indexer getAssociationIndexer(Association oneToMany) {
        return new RedisAssociationIndexer(jredisClient, typeConverter, oneToMany);
    }
}
