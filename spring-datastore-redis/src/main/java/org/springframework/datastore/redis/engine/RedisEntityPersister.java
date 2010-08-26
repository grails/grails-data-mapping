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

import org.springframework.beans.SimpleTypeConverter;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.datastore.engine.AssociationIndexer;
import org.springframework.datastore.engine.PropertyValueIndexer;
import org.springframework.datastore.keyvalue.engine.AbstractKeyValueEntityPesister;
import org.springframework.datastore.mapping.MappingContext;
import org.springframework.datastore.mapping.PersistentEntity;
import org.springframework.datastore.mapping.PersistentProperty;
import org.springframework.datastore.mapping.types.Association;
import org.springframework.datastore.query.Query;
import org.springframework.datastore.redis.RedisEntry;
import org.springframework.datastore.redis.RedisSession;
import org.springframework.datastore.redis.collection.RedisCollection;
import org.springframework.datastore.redis.collection.RedisSet;
import org.springframework.datastore.redis.query.RedisQuery;
import org.springframework.datastore.redis.util.RedisTemplate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * An {@link org.springframework.datastore.engine.EntityPersister} for the Redis NoSQL datastore
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class RedisEntityPersister extends AbstractKeyValueEntityPesister<RedisEntry, Long> {
    private RedisAssociationIndexer indexer;
    private RedisTemplate redisTemplate;
    private RedisCollection allEntityIndex;

    public RedisEntityPersister(MappingContext context, PersistentEntity entity, RedisSession conn, final RedisTemplate template) {
        super(context, entity, conn);
        this.redisTemplate = template;
        allEntityIndex = new RedisSet(redisTemplate, getEntityFamily() + ".all");
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
        if(value != null) {
            nativeEntry.put(key, value.toString());
        }
    }

    @Override
    public boolean isLocked(Object o) {
        return super.isLocked(o);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    protected void lockEntry(PersistentEntity persistentEntity, String entityFamily, Serializable id, int timeout) {
        String redisKey = getRedisKey(entityFamily, id);
        final TimeUnit milliUnit = TimeUnit.MILLISECONDS;
        final long waitTime = TimeUnit.SECONDS.toMillis(timeout);
        final String lockName = lockName(redisKey);
        int sleepTime = 0;
        while(true) {
            if(redisTemplate.setnx(lockName, System.currentTimeMillis()) && redisTemplate.expire(lockName, timeout)) {
                    break;
            }
            else {
                if(redisTemplate.ttl(lockName) > 0) {
                    try {
                        if(sleepTime > waitTime) {
                            throw new CannotAcquireLockException("Failed to acquire lock on key ["+redisKey+"]. Wait time exceeded timeout.");
                        }
                        else {
                            // wait for previous lock to expire
                            sleepTime += 500;
                            milliUnit.sleep(500);
                        }
                    } catch (InterruptedException e) {
                        throw new CannotAcquireLockException("Failed to acquire lock on key ["+redisKey+"]: " + e.getMessage(), e);
                    }
                }
                else {
                    if(redisTemplate.getset(lockName, System.currentTimeMillis()) != null 
                            && redisTemplate.expire(lockName, timeout)) break;
                }
            }

        }
    }

    private String lockName(String redisKey) {
        return redisKey + ".lock";
    }

    @Override
    protected void unlockEntry(PersistentEntity persistentEntity, String entityFamily, Serializable id) {

        String redisKey = getRedisKey(entityFamily, id);
        redisTemplate.del(lockName(redisKey));
    }

    @Override
    protected RedisEntry retrieveEntry(PersistentEntity persistentEntity, final String family, Serializable key) {
        final String hashKey = getRedisKey(family, key);

        final List<String> props = persistentEntity.getPersistentPropertyNames();
        final String[] values = redisTemplate.hmget(hashKey, props.toArray(new String[props.size()]));
        if(entityDoesntExistForValues(values)) return null;
        RedisEntry entry = new RedisEntry(family);
        for (int i = 0; i < props.size(); i++) {
              entry.put(props.get(i), values[i]);
        }
        return entry;
    }

    private String getRedisKey(String family, Serializable key) {
        return family + ":" + getLong(key);
    }

    private boolean entityDoesntExistForValues(String[] values) {
        if(values == null || values.length == 0) return false;
        for (String value : values) {
            if(value != null) return false;
        }
        return true;
    }


    @Override
    protected void updateEntry(PersistentEntity persistentEntity, Long key, RedisEntry nativeEntry) {
        String family = getFamily(persistentEntity, persistentEntity.getMapping());
        performInsertion(family, key, nativeEntry);
    }

    @Override
    protected Long storeEntry(PersistentEntity persistentEntity, Long storeId, RedisEntry nativeEntry) {
        final String family = nativeEntry.getFamily();
        return performInsertion(family, storeId, nativeEntry);
    }

    @Override
    protected Long generateIdentifier(PersistentEntity persistentEntity, RedisEntry id) {
        String family = getFamily(persistentEntity, persistentEntity.getMapping());
        return generateIdentifier(family);
    }

    private Long performInsertion(final String family, final Long id, final RedisEntry nativeEntry) {
        String key = family + ":" + id;
        redisTemplate.hmset(key,nativeEntry);
        return id;

    }

    public RedisCollection getAllEntityIndex() {
        return this.allEntityIndex;
    }

    protected Long generateIdentifier(final String family) {
        long id = redisTemplate.incr(family + ".next_id");
        getAllEntityIndex().add(id);
        return id;
    }

    @Override
    protected void deleteEntries(final String family, final List<Long> keys) {
        final List<String> actualKeys = new ArrayList<String>();
        for (Long key : keys) {
            actualKeys.add(family + ":" + key);
            getAllEntityIndex().remove(key);
        }

        redisTemplate.del(actualKeys.toArray(new String[actualKeys.size()]));
    }

    @Override
    protected void deleteEntry(final String family, final Long key) {
        final String actualKey = family + ":" + key;
        getAllEntityIndex().remove(key);
        redisTemplate.del(actualKey);
    }

    @Override
    public PropertyValueIndexer getPropertyIndexer(PersistentProperty property) {
        return new RedisPropertyValueIndexer(this, property);
    }

    @Override
    public AssociationIndexer getAssociationIndexer(Association oneToMany) {
        return new RedisAssociationIndexer(redisTemplate, typeConverter, oneToMany);
    }

    @Override
    protected List<Object> retrieveAllEntities(PersistentEntity persistentEntity, Iterable<Serializable> keys) {

        // TODO: Performance wise this sucks. Replace with lazy ResultList implementation that loads results on demand
        // This could still result in an N+1 scenario, however it is better than what we have right now.
        // Ideally replace this with some bulk retrieval mechanism when Redis supports it
        return super.retrieveAllEntities(persistentEntity, keys);
    }

    public Query createQuery() {
        return new RedisQuery((RedisSession) session, getRedisTemplate(), getPersistentEntity(), this);
    }

    public RedisTemplate getRedisTemplate() {
        return this.redisTemplate;
    }

    public SimpleTypeConverter getTypeConverter() {
        return typeConverter;
    }


    public String getEntityBaseKey() {
        return getFamily(getPersistentEntity(), getPersistentEntity().getMapping());
    }

    public String getPropertySortKey(PersistentProperty property) {
        return "~" + getEntityBaseKey() + ":" + property.getName() + ":sorted";
    }
}
