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
import org.grails.datastore.gorm.SessionCallback
import org.grails.datastore.gorm.VoidSessionCallback
import org.springframework.datastore.mapping.core.Datastore
import org.springframework.datastore.mapping.core.Session
import org.springframework.transaction.PlatformTransactionManager

/**
 * Adds Redis specific functionality to GORM.
 */
class RedisGormEnhancer extends GormEnhancer {

    RedisGormEnhancer(Datastore datastore) {
        super(datastore)
    }

    RedisGormEnhancer(Datastore datastore, PlatformTransactionManager transactionManager) {
        super(datastore, transactionManager)
    }

    protected <D> GormStaticApi<D> getStaticApi(Class<D> cls) {
        return new RedisGormStaticApi<D>(cls, datastore)
    }

    protected <D> GormInstanceApi<D> getInstanceApi(Class<D> cls) {
        return new RedisGormInstanceApi<D>(cls, datastore)
    }
}

class RedisGormInstanceApi<D> extends GormInstanceApi<D> {

    RedisGormInstanceApi(Class<D> persistentClass, Datastore datastore) {
        super(persistentClass, datastore)
    }

    void expire(D instance, int ttl) {
        execute new VoidSessionCallback() {
            void doInSession(Session session) {
                session.expire instance, ttl
            }
        }
    }
}

class RedisGormStaticApi<D> extends GormStaticApi<D> {

    RedisGormStaticApi(Class<D> persistentClass, Datastore datastore) {
        super(persistentClass, datastore)
    }

    /**
     * Expires an entity for the given id and TTL.
     */
    void expire(Serializable id, int ttl) {
        execute new VoidSessionCallback() {
            void doInSession(Session session) {
                session.expire(persistentClass, id, ttl)
            }
        }
    }

    /**
     * A random domain class instance is returned.
     * @return A random domain class
     */
    D random() {
        execute new SessionCallback() {
            def doInSession(Session session) {
                session.random(persistentClass)
            }
        }
    }

    /**
     * A random domain class instance is removed and returned.
     * @return A random removed domain class
     */
    D pop() {
        execute new SessionCallback() {
            def doInSession(Session session) {
                session.pop(persistentClass)
            }
        }
    }
}
