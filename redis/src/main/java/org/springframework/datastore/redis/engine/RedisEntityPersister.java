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

import org.jredis.JRedis;
import org.jredis.RedisException;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.datastore.engine.AssociationIndexer;
import org.springframework.datastore.engine.PropertyValueIndexer;
import org.springframework.datastore.keyvalue.engine.AbstractKeyValueEntityPesister;
import org.springframework.datastore.mapping.PersistentEntity;
import org.springframework.datastore.mapping.PersistentProperty;
import org.springframework.datastore.mapping.types.Association;
import org.springframework.datastore.query.Query;
import org.springframework.datastore.redis.RedisEntry;
import org.springframework.datastore.redis.RedisSession;
import org.springframework.datastore.redis.query.RedisQuery;
import org.springframework.datastore.redis.util.RedisCallback;
import org.springframework.datastore.redis.util.RedisTemplate;

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
    private RedisAssociationIndexer indexer;
    private RedisTemplate redisTemplate;

    public RedisEntityPersister(PersistentEntity entity, RedisSession conn, final JRedis jredisClient) {
        super(entity, conn);
        this.redisTemplate = new RedisTemplate(jredisClient);
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
    protected RedisEntry retrieveEntry(PersistentEntity persistentEntity, final String family, Serializable key) {
        final String hashKey = family + ":" + getLong(key);

        final List<String> props = persistentEntity.getPersistentPropertyNames();
        return (RedisEntry) redisTemplate.execute(new RedisCallback() {
            public Object doInRedis(JRedis jredis) throws RedisException {
                final List<byte[]> values = jredis.hmget(hashKey, props.toArray(new String[props.size()]));
                if(entityDoesntExistForValues(values)) return null;
                RedisEntry entry = new RedisEntry(family);
                for (int i = 0; i < props.size(); i++) {
                      entry.put(props.get(i), values.get(i));
                }
                return entry;
            }
        });
    }

    private boolean entityDoesntExistForValues(List<byte[]> values) {
        return values == null || values.size() == 0 || (values.size() == 1 && values.get(0) == null);
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

    private Long performInsertion(final String family, final Long id, final RedisEntry nativeEntry) {
        return (Long) redisTemplate.execute(new RedisCallback() {
            public Object doInRedis(JRedis jredis) throws RedisException {
               String key = family + ":" + id;
               jredis.hmset(key,nativeEntry);
               return id;
            }
        });
    }

    public String getAllEntityIndex() {
        return getEntityFamily() + ".all";
    }

    protected Long generateIdentifier(final String family) {
        return (Long) redisTemplate.execute(new RedisCallback(){
            public Object doInRedis(JRedis jredis) throws RedisException {
                final long id = jredis.incr(family + ".next_id");
                // keep a record of all inserted entities for querying later
                jredis.rpush(getAllEntityIndex(), id);
                return id;
            }
        });
    }

    @Override
    protected void deleteEntries(final String family, final List<Long> keys) {
        redisTemplate.execute(new RedisCallback(){
            public Object doInRedis(JRedis jredis) throws RedisException {
                final List<String> actualKeys = new ArrayList<String>();
                for (Long key : keys) {
                    actualKeys.add(family + ":" + key);
                    jredis.lrem(getAllEntityIndex(), key, 0);
                }

                jredis.del(actualKeys.toArray(new String[actualKeys.size()]));

                return null;
            }
        });
    }

    @Override
    protected void deleteEntry(final String family, final Long key) {
        final String actualKey = family + ":" + key;
        redisTemplate.execute(new RedisCallback(){
            public Object doInRedis(JRedis jredis) throws RedisException {
                jredis.lrem(getAllEntityIndex(), key, 0);
                jredis.del(actualKey);
                return null;
            }
        });
    }

    @Override
    protected PropertyValueIndexer getPropertyIndexer(PersistentProperty property) {
        return new RedisPropertyValueIndexer(redisTemplate.getJRedis(),typeConverter, property);
    }

    @Override
    protected AssociationIndexer getAssociationIndexer(Association oneToMany) {
        return new RedisAssociationIndexer(redisTemplate.getJRedis(), typeConverter, oneToMany);
    }

    public Query createQuery() {
        return new RedisQuery(getPersistentEntity(), this);
    }

    public RedisTemplate getRedisTemplate() {
        return this.redisTemplate;
    }

    public SimpleTypeConverter getTypeConverter() {
        return typeConverter;
    }
}
