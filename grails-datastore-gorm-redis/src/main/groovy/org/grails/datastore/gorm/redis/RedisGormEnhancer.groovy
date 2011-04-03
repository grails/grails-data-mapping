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
package org.grails.datastore.gorm.redis

import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormInstanceApi
import org.grails.datastore.gorm.GormStaticApi
import org.springframework.datastore.mapping.core.Datastore
import org.springframework.datastore.mapping.redis.RedisSession
import org.springframework.transaction.PlatformTransactionManager

/**
 * Adds Redis specific functionality to GORM.
 */
class RedisGormEnhancer extends GormEnhancer {

    RedisGormEnhancer(Datastore datastore) {
        super(datastore);
    }

    RedisGormEnhancer(Datastore datastore, PlatformTransactionManager transactionManager) {
        super(datastore, transactionManager);
    }

    protected GormStaticApi getStaticApi(Class cls) {
        return new RedisGormStaticApi(cls, datastore)
    }

    protected GormInstanceApi getInstanceApi(Class cls) {
        return new RedisGormInstanceApi(cls, datastore)
    }
}

class RedisGormInstanceApi extends GormInstanceApi {

    RedisGormInstanceApi(Class persistentClass, Datastore datastore) {
        super(persistentClass, datastore);
    }

    def expire(instance, int ttl) {
        datastore.currentSession.expire instance, ttl
    }
}

class RedisGormStaticApi extends GormStaticApi {
    RedisGormStaticApi(Class persistentClass, Datastore datastore) {
        super(persistentClass, datastore);
    }

    /**
     * Expires an entity for the given id and TTL.
     */
    void expire(Serializable id, int ttl) {
        datastore.currentSession.expire(persistentClass, id, ttl)
    }

    /**
     * A random domain class instance is returned.
     * @return A random domain class
     */
    def random() {
        datastore.currentSession.random(persistentClass)
    }

    /**
     * A random domain class instance is removed and returned.
     * @return A random removed domain class
     */
    def pop() {
        datastore.currentSession.pop(persistentClass)
    }
}
