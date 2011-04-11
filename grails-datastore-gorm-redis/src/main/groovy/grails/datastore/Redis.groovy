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
package grails.datastore

import org.springframework.datastore.mapping.core.Datastore
import org.springframework.datastore.mapping.core.Session
import org.springframework.datastore.mapping.model.PersistentEntity
import org.springframework.datastore.mapping.redis.collection.RedisCollection
import org.springframework.datastore.mapping.redis.collection.RedisList
import org.springframework.datastore.mapping.redis.collection.RedisMap
import org.springframework.datastore.mapping.redis.collection.RedisSet
import org.springframework.datastore.mapping.redis.util.RedisTemplate

/**
 * Convenience interface for access the Redis datastore.
 *
 * @author Graeme Rocher
 */
class Redis {

    @Delegate RedisTemplate redisTemplate
    private Datastore datastore

    def getAt(String s) {
        get(s)
    }

    def setAt(String s, v) {
        set s, v
    }

    void setDatastore(Datastore ds) {
        datastore = ds
        boolean existing = datastore.hasCurrentSession()
        Session session = datastore.currentSession
        redisTemplate = session.nativeInterface
        if (!existing) {
            session.disconnect()
        }
    }

    /**
     * Creates a Redis set for the given key.
     * @param key the key
     * @return the set
     */
    RedisSet set(String key) {
        return new RedisSet(redisTemplate, key)
    }

    /**
     * Creates a hash for the given key.
     * @param key the key
     * @return the hash
     */
    RedisMap hash(String key) {
        return new RedisMap(redisTemplate, key)
    }

    /**
     * Creates a Redis list for the given key.
     * @param key the key
     * @return the list
     */
    RedisList list(String key) {
        return new RedisList(redisTemplate, key)
    }

    /**
     * Returns an entity list from the specified key.
     * @param key the key
     * @return An entity list
     */
    Collection entities(Class type, String key, int offset = 0, int max = -1) {
        def set = set(key)
        return entities(type, set, offset, max)
    }

    /**
     * Returns entities from the specified type and Redis collection.
     * @param type The type
     * @param col The collection
     * @param offset The offset
     * @param max The max
     * @return entities
     */
    Collection entities(Class type, RedisCollection col, int offset = 0, int max = -1) {
        PersistentEntity entity = datastore.mappingContext.getPersistentEntity(type.name)
        if (entity == null) {
            throw new IllegalArgumentException("Class [$type] is not a persistent entity")
        }
        def results = col.members(offset, max)
        datastore.currentSession.retrieveAll(type, results)
    }
}
