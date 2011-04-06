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
package org.springframework.datastore.mapping.redis.engine;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.convert.ConversionService;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.datastore.mapping.engine.AssociationIndexer;
import org.springframework.datastore.mapping.engine.PropertyValueIndexer;
import org.springframework.datastore.mapping.keyvalue.engine.AbstractKeyValueEntityPesister;
import org.springframework.datastore.mapping.model.MappingContext;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.model.PersistentProperty;
import org.springframework.datastore.mapping.model.types.Association;
import org.springframework.datastore.mapping.proxy.EntityProxy;
import org.springframework.datastore.mapping.query.Query;
import org.springframework.datastore.mapping.redis.RedisEntry;
import org.springframework.datastore.mapping.redis.RedisSession;
import org.springframework.datastore.mapping.redis.collection.RedisCollection;
import org.springframework.datastore.mapping.redis.collection.RedisSet;
import org.springframework.datastore.mapping.redis.query.RedisQuery;
import org.springframework.datastore.mapping.redis.util.RedisCallback;
import org.springframework.datastore.mapping.redis.util.RedisTemplate;

/**
 * An {@link org.springframework.datastore.mapping.engine.EntityPersister} for the Redis NoSQL datastore
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class RedisEntityPersister extends AbstractKeyValueEntityPesister<Map, Long> {
    private RedisTemplate redisTemplate;
    private RedisCollection allEntityIndex;

    public static final String DISCRIMINATOR = "discriminator";

    public RedisEntityPersister(MappingContext context, PersistentEntity entity, RedisSession conn,
           final RedisTemplate template, ApplicationEventPublisher publisher) {
        super(context, entity, conn, publisher);
        this.redisTemplate = template;
        allEntityIndex = new RedisSet(redisTemplate, getEntityFamily() + ".all");
    }

    private Long getLong(Object key) {
        return getMappingContext().getConversionService().convert(key, Long.class);
    }

    @Override
    protected RedisEntry createNewEntry(String family) {
        return new RedisEntry(family);
    }

    @Override
    protected Object getEntryValue(Map nativeEntry, String property) {
        return nativeEntry.get(property);
    }

    @Override
    protected void setEntryValue(Map nativeEntry, String key, Object value) {
        if (value == null || !shouldConvert(value)) {
            return;
        }

        final ConversionService conversionService = getMappingContext().getConversionService();
        nativeEntry.put(key, conversionService.convert(value, String.class));
    }

    private boolean shouldConvert(Object value) {
        return !getMappingContext().isPersistentEntity(value) && !(value instanceof EntityProxy);
    }

    @Override
    public boolean isLocked(Object o) {
        return super.isLocked(o);
    }

    @Override
    protected void lockEntry(PersistentEntity persistentEntity, @SuppressWarnings("hiding") String entityFamily,
            Serializable id, int timeout) {
        String redisKey = getRedisKey(entityFamily, id);
        final TimeUnit milliUnit = TimeUnit.MILLISECONDS;
        final long waitTime = TimeUnit.SECONDS.toMillis(timeout);
        final String lockName = lockName(redisKey);
        int sleepTime = 0;
        while(true) {
            if (redisTemplate.setnx(lockName, System.currentTimeMillis()) && redisTemplate.expire(lockName, timeout)) {
                break;
            }
            if (redisTemplate.ttl(lockName) > 0) {
                try {
                    if (sleepTime > waitTime) {
                        throw new CannotAcquireLockException("Failed to acquire lock on key ["+redisKey+"]. Wait time exceeded timeout.");
                    }
                    // wait for previous lock to expire
                    sleepTime += 500;
                    milliUnit.sleep(500);
                } catch (InterruptedException e) {
                    throw new CannotAcquireLockException("Failed to acquire lock on key ["+redisKey+"]: " + e.getMessage(), e);
                }
            }
            else {
                if (redisTemplate.getset(lockName, System.currentTimeMillis()) != null &&
                        redisTemplate.expire(lockName, timeout)) {
                    break;
                }
            }
        }
    }

    private String lockName(String redisKey) {
        return redisKey + ".lock";
    }

    @Override
    protected void unlockEntry(PersistentEntity persistentEntity,
            @SuppressWarnings("hiding") String entityFamily, Serializable id) {
        String redisKey = getRedisKey(entityFamily, id);
        redisTemplate.del(lockName(redisKey));
    }

    @Override
    protected Serializable convertToNativeKey(Serializable nativeKey) {
        return getMappingContext().getConversionService().convert(nativeKey, Long.class);
    }

    @Override
    protected PersistentEntity discriminatePersistentEntity(PersistentEntity persistentEntity, Map nativeEntry) {
        if (!nativeEntry.containsKey(DISCRIMINATOR)) {
            return persistentEntity;
        }

        String discriminator = nativeEntry.get(DISCRIMINATOR).toString();
        final PersistentEntity childEntity = getMappingContext().getChildEntityByDiscriminator(persistentEntity.getRootEntity(), discriminator);
        return childEntity == null ? persistentEntity : childEntity;
    }

    @Override
    protected Map retrieveEntry(PersistentEntity persistentEntity, final String family, Serializable key) {
        String hashKey = getEntryKey(persistentEntity, family, key);

        final Map map = redisTemplate.hgetall(hashKey);
        return map == null || map.isEmpty() ? null : map;
    }

    private String getEntryKey(PersistentEntity persistentEntity, String family, Serializable key) {
        String hashKey;
        if (persistentEntity.isRoot()) {
            hashKey = getRedisKey(family, key);
        }
        else {
            RedisEntityPersister persister = (RedisEntityPersister) session.getPersister(persistentEntity.getRootEntity());
            hashKey = getRedisKey(persister.getFamily(), key);
        }
        return hashKey;
    }

    @Override
    protected List<Object> retrieveAllEntities(final PersistentEntity persistentEntity, Iterable<Serializable> keys) {

        List<Object> entityResults = new ArrayList<Object>();

        if (keys == null) {
            return entityResults;
        }

        final List<Serializable> redisKeys;
        if (keys instanceof List) {
            redisKeys = (List<Serializable>) keys;
        }
        else {
            redisKeys = new ArrayList<Serializable>();
            for (Serializable key : keys) {
                 redisKeys.add(key);
            }
        }

        if (redisKeys.isEmpty()) {
            return entityResults;
        }

        List<Object> results = redisTemplate.pipeline(new RedisCallback<RedisTemplate>() {
            public Object doInRedis(RedisTemplate redis) throws IOException {
                for (Serializable key : redisKeys) {
                    redis.hgetall(getEntryKey(persistentEntity, getFamily(), key));
                }
                return null;
            }
        });

        for (int i = 0, count = redisKeys.size(); i < count; i++) {
            Serializable nativeKey = redisKeys.get(i);
            Map nativeEntry = getNativeEntryFromList(results.get(i));
            entityResults.add(createObjectFromNativeEntry(getPersistentEntity(), nativeKey, nativeEntry));
        }
        return entityResults;
    }

    private Map getNativeEntryFromList(Object result) {
        Collection flatHash = (Collection) result;
        Map<String, String> hash = new HashMap<String, String>();
        Iterator<String> iterator = flatHash.iterator();
        while (iterator.hasNext()) {
            hash.put(iterator.next(), iterator.next());
        }
        return hash;
    }

    private String getRedisKey(String family, Serializable key) {
        return family + ":" + getLong(key);
    }

    @Override
    public void updateEntry(PersistentEntity persistentEntity, Long key, Map nativeEntry) {
        try {
            if (!persistentEntity.isRoot()) {
                performInsertion(getRootFamily(persistentEntity), key, nativeEntry);
            }
            else {
                performInsertion(getFamily(), key, nativeEntry);
            }
        } finally {
            updateAllEntityIndex(persistentEntity, key);
        }
    }

    @Override
    public Long storeEntry(PersistentEntity persistentEntity, Long storeId, Map nativeEntry) {
        try {
            if (!persistentEntity.isRoot()) {
                nativeEntry.put(DISCRIMINATOR, persistentEntity.getDiscriminator());
                return performInsertion(getRootFamily(persistentEntity), storeId, nativeEntry);
            }
            return performInsertion(getFamily(), storeId, nativeEntry);
        }
        finally {
            updateAllEntityIndex(persistentEntity, storeId);
        }
    }

    private void updateAllEntityIndex(PersistentEntity persistentEntity, Long storeId) {
        getAllEntityIndex().add(storeId);
        PersistentEntity parent = persistentEntity.getParentEntity();
        while (parent != null) {
            RedisEntityPersister persister = (RedisEntityPersister) session.getPersister(parent);
            persister.getAllEntityIndex().add(storeId);
            parent = parent.getParentEntity();
        }
    }

    private String getRootFamily(PersistentEntity persistentEntity) {
        final PersistentEntity root = persistentEntity.getRootEntity();
        final RedisEntityPersister persister = (RedisEntityPersister) session.getPersister(root);
        return persister.getFamily();
    }

    @Override
    protected Long generateIdentifier(PersistentEntity persistentEntity, Map entry) {
        // always use the root of an inheritance hierarchy to generate the identifier
        PersistentEntity root = persistentEntity.getRootEntity();
        RedisEntityPersister persister = (RedisEntityPersister) session.getPersister(root);

        return generateIdentifier(persister.getFamily());
    }

    private Long performInsertion(final String family, final Long id, final Map nativeEntry) {
        String key = family + ":" + id;
        redisTemplate.hmset(key,nativeEntry);
        return id;
    }

    public RedisCollection getAllEntityIndex() {
        return allEntityIndex;
    }

    public String getFamily() {
        return entityFamily;
    }

    protected Long generateIdentifier(final String family) {
        return (long) redisTemplate.incr(family + ".next_id");
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
        return new RedisPropertyValueIndexer(getMappingContext(),this, property);
    }

    @Override
    public AssociationIndexer getAssociationIndexer(Map nativeEntry, Association oneToMany) {
        return new RedisAssociationIndexer(redisTemplate, getMappingContext().getConversionService(), oneToMany);
    }

    public Query createQuery() {
        return new RedisQuery((RedisSession) session, getRedisTemplate(), getPersistentEntity(), this);
    }

    public RedisTemplate getRedisTemplate() {
        return redisTemplate;
    }

    public String getEntityBaseKey() {
        return getFamily(getPersistentEntity(), getPersistentEntity().getMapping());
    }

    public String getPropertySortKey(PersistentProperty property) {
        return getEntityBaseKey() + ":" + property.getName() + ":sorted";
    }

    public String getPropertySortKeyPattern() {
        return getEntityBaseKey() + ":*:sorted";
    }

    public String getRedisKey(Serializable key) {
        return getRedisKey(getFamily(), key);
    }
}
