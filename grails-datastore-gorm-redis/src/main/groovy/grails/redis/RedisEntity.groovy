/*
 * Copyright 2015 original authors
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
package grails.redis

import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormEntity
import org.grails.datastore.mapping.core.AbstractDatastore
import org.grails.datastore.mapping.redis.RedisDatastore
import org.grails.datastore.mapping.redis.RedisSession


/**
 * Extends {@link org.grails.datastore.gorm.GormEntity} with additional methods specific to Redis
 *
 * @author Graeme Rocher
 * @since 5.0
 */
trait RedisEntity<D> extends GormEntity<D> {

    void expire(int ttl) {
        RedisSession session = (RedisSession)AbstractDatastore.retrieveSession(RedisDatastore)
        session.expire this, ttl
    }

    /**
     * Expires an entity for the given id and TTL.
     */
    static void expire(Serializable id, int ttl) {
        withSession( { RedisSession session ->
            def entity = session.mappingContext.getPersistentEntity(this.name)
            def persistentClass = entity.javaClass
            session.expire(persistentClass, id, ttl)
        })
    }

    /**
     * A random domain class instance is returned.
     * @return A random domain class
     */
    static D random() {
        withSession( { RedisSession session ->
            def entity = session.mappingContext.getPersistentEntity(this.name)
            def persistentClass = entity.javaClass
            (D)session.random(persistentClass)
        })
    }

    /**
     * A random domain class instance is removed and returned.
     * @return A random removed domain class
     */
    static D pop() {
        withSession( { RedisSession session ->
            def entity = session.mappingContext.getPersistentEntity(this.name)
            def persistentClass = entity.javaClass
            (D)session.pop(persistentClass)
        })
    }

    /**
     * Execute a closure whose first argument is a reference to the current session.
     *
     * @param callable the closure
     * @return The result of the closure
     */
    static <T> T withSession(Closure<T> callable) {
        GormEnhancer.findStaticApi(this).withSession callable
    }
}