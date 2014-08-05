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
package org.grails.datastore.mapping.redis.engine;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.grails.datastore.mapping.core.OptimisticLockingException;
import org.grails.datastore.mapping.core.SessionImplementor;
import org.grails.datastore.mapping.engine.AssociationIndexer;
import org.grails.datastore.mapping.engine.EntityAccess;
import org.grails.datastore.mapping.engine.PropertyValueIndexer;
import org.grails.datastore.mapping.keyvalue.engine.AbstractKeyValueEntityPersister;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.types.Association;
import org.grails.datastore.mapping.proxy.EntityProxy;
import org.grails.datastore.mapping.query.Query;
import org.grails.datastore.mapping.redis.RedisEntry;
import org.grails.datastore.mapping.redis.RedisSession;
import org.grails.datastore.mapping.redis.collection.RedisCollection;
import org.grails.datastore.mapping.redis.collection.RedisSet;
import org.grails.datastore.mapping.redis.query.RedisQuery;
import org.grails.datastore.mapping.redis.util.RedisCallback;
import org.grails.datastore.mapping.redis.util.RedisTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.convert.ConversionService;
import org.springframework.dao.CannotAcquireLockException;

import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.util.SafeEncoder;

/**
 * An {@link org.grails.datastore.mapping.engine.EntityPersister} for the Redis NoSQL datastore.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class RedisEntityPersister extends AbstractKeyValueEntityPersister<Map, Long> {
    public static final String UTF_8 = "UTF-8";
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
    protected EntityAccess createEntityAccess(PersistentEntity persistentEntity, Object obj, Map nativeEntry) {
        final NativeEntryModifyingEntityAccess ea = new NativeEntryModifyingEntityAccess(persistentEntity, obj) {
            public void setProperty(String name, Object value) {
                Class type = getPropertyType(name);
                if(type.isArray() && byte.class.isAssignableFrom(type.getComponentType()) && value instanceof CharSequence) {
                    try {
                        super.setProperty(name, value.toString().getBytes(Protocol.CHARSET));
                    } catch (UnsupportedEncodingException e) {
                        // ignore
                    }
                }
                else {
                    super.setProperty(name, value);
                }

            }
        };
        ea.setConversionService(getMappingContext().getConversionService());
        ea.setNativeEntry(nativeEntry);
        return ea;

    }

    @Override
    protected void setEntryValue(Map nativeEntry, String key, Object value) {
        if (value == null || !shouldConvert(value)) {
            return;
        }

        Class type = value.getClass();
        if(value != null && type.isArray() && byte.class.isAssignableFrom(type.getComponentType())) {
            nativeEntry.put(key, SafeEncoder.encode((byte[])value));
        }
        else {

            final ConversionService conversionService = getMappingContext().getConversionService();
            nativeEntry.put(key, conversionService.convert(value, String.class));
        }
    }

    private boolean shouldConvert(Object value) {
        return !getMappingContext().isPersistentEntity(value) && !(value instanceof EntityProxy);
    }

    @Override
    protected void lockEntry(PersistentEntity persistentEntity, String entityFamily, Serializable id, int timeout) {
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
    protected void unlockEntry(PersistentEntity persistentEntity, String entityFamily, Serializable id) {
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

        Iterator<Serializable> keyIter = redisKeys.iterator();
        Iterator resultIter = results.iterator();
        while (keyIter.hasNext() && resultIter.hasNext()) {
            Serializable nativeKey = keyIter.next();
            Object entity;
            SessionImplementor sessionImplementor = (SessionImplementor)session;
            if (sessionImplementor.isCached(persistentEntity.getJavaClass(), nativeKey)) {
                entity = sessionImplementor.getCachedInstance(persistentEntity.getJavaClass(), nativeKey);
            }
            else {
                Map nativeEntry = (Map)resultIter.next();
                entity = createObjectFromNativeEntry(persistentEntity, nativeKey, nativeEntry);
                sessionImplementor.cacheInstance(persistentEntity.getJavaClass(), nativeKey, entity);
            }
            entityResults.add(entity);
        }
        return entityResults;
    }

    private String getRedisKey(String family, Serializable key) {
        return family + ":" + getLong(key);
    }

    @Override
    public void updateEntry(final PersistentEntity persistentEntity, final EntityAccess entityAccess,
                            final Long key, final Map nativeEntry) {
        try {
            if (!persistentEntity.isRoot()) {
                saveOrUpdate(getRootFamily(persistentEntity), key, nativeEntry, persistentEntity, entityAccess, true);
            }
            else {
                saveOrUpdate(getFamily(), key, nativeEntry, persistentEntity, entityAccess, true);
            }
        } finally {
            updateAllEntityIndex(persistentEntity, key);
        }
    }

    @Override
    public Long storeEntry(final PersistentEntity persistentEntity, final EntityAccess entityAccess,
                           final Long storeId, final Map nativeEntry) {
        try {
            if (!persistentEntity.isRoot()) {
                nativeEntry.put(DISCRIMINATOR, persistentEntity.getDiscriminator());
                saveOrUpdate(getRootFamily(persistentEntity), storeId, nativeEntry, persistentEntity, entityAccess, false);
                return storeId;
            }
            saveOrUpdate(getFamily(), storeId, nativeEntry, persistentEntity, entityAccess, false);
            return storeId;
        }
        finally {
            updateAllEntityIndex(persistentEntity, storeId);
        }
    }

    private void updateAllEntityIndex(PersistentEntity persistentEntity, Long storeId) {
        try {
            getAllEntityIndex().add(storeId);
        }
        catch (JedisDataException e) {
            handleJedisDataException(e);
        }
        PersistentEntity parent = persistentEntity.getParentEntity();
        while (parent != null) {
            RedisEntityPersister persister = (RedisEntityPersister) session.getPersister(parent);
            try {
                persister.getAllEntityIndex().add(storeId);
            }
            catch (JedisDataException e) {
                handleJedisDataException(e);
            }
            parent = parent.getParentEntity();
        }
    }

    private void handleJedisDataException(JedisDataException e) {
        // TODO horrible hack workaround for a Jedis 2.0.0 bug; the command works but the
        //      client doesn't handle the response correctly, so it's safe to ignore the
        //      exception if we're in a transaction
        if (e.getMessage().startsWith("Please close pipeline or multi block before calling this method")) {
            if (!getRedisTemplate().isInMulti()) {
                throw e;
            }
        }
        else {
            throw e;
        }
    }

    private String getRootFamily(PersistentEntity persistentEntity) {
        final PersistentEntity root = persistentEntity.getRootEntity();
        return ((RedisEntityPersister)session.getPersister(root)).getFamily();
    }

    @Override
    protected Long generateIdentifier(PersistentEntity persistentEntity, Map entry) {
        // always use the root of an inheritance hierarchy to generate the identifier
        PersistentEntity root = persistentEntity.getRootEntity();
        RedisEntityPersister persister = (RedisEntityPersister) session.getPersister(root);

        return generateIdentifier(persister.getFamily());
    }

    private void saveOrUpdate(final String family, final Long id, final Map nativeEntry,
            final PersistentEntity persistentEntity, final EntityAccess entityAccess, final boolean update) {

        final String key = family + ":" + id;

        if (update && isVersioned(entityAccess)) {
            String oldVersion;
            RedisSession newSession = (RedisSession)getSession().getDatastore().connect();
            try {
                oldVersion = newSession.getNativeInterface().hget(key, "version");
            }
            finally {
                newSession.disconnect();
            }

            String version = (String)nativeEntry.get("version");
            if (oldVersion != null && version != null && !version.equals(oldVersion)) {
                throw new OptimisticLockingException(persistentEntity, id);
            }

            incrementVersion(entityAccess);
        }

        redisTemplate.hmset(key, nativeEntry);
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
    protected void deleteEntry(final String family, final Long key, final Object entry) {
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
