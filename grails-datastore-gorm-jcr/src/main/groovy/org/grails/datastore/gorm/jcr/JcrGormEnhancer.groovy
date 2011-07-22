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
package org.grails.datastore.gorm.jcr

import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormInstanceApi
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.jcr.JcrSession
import org.springframework.transaction.PlatformTransactionManager

/**
 * Adds JCR specific functionality to GORM
 *
 * @author Erawat Chamanont
 * @since 1.0
 */
class JcrGormEnhancer extends GormEnhancer{

    JcrGormEnhancer(Datastore datastore) {
        super(datastore);
    }

    JcrGormEnhancer(Datastore datastore, PlatformTransactionManager transactionManager) {
        super(datastore, transactionManager);
    }

    protected GormStaticApi getStaticApi(Class cls) {
        return new JcrGormStaticApi(cls, datastore, finders)
    }

    protected GormInstanceApi getInstanceApi(Class cls) {
        return new JcrGormInstanceApi(cls, datastore)
    }
}
class JcrGormInstanceApi extends GormInstanceApi {

    JcrGormInstanceApi(Class persistentClass, Datastore datastore) {
        super(persistentClass, datastore);
    }

    def expire(instance, int ttl) {
        JcrSession session = datastore.currentSession

        session.expire instance, ttl
    }
}

class JcrGormStaticApi extends GormStaticApi {
    JcrGormStaticApi(Class persistentClass, Datastore datastore, List<FinderMethod> finders) {
        super(persistentClass, datastore, finders);
    }

    /**
     * Expires an entity for the given id and TTL
     */
    void expire(Serializable id, int ttl) {
        JcrSession session = datastore.currentSession

        session.expire(persistentClass, id, ttl)
    }

    /**
     * A random domain class instance is returned
     * @return A random domain class
     */
    def random() {
        JcrSession session = datastore.currentSession

        return session.random(persistentClass)
    }

    /**
     * A random domain class instance is removed and returned
     * @return A random removed domain class
     */
    def pop() {
        JcrSession session = datastore.currentSession

        return session.pop(persistentClass)
    }
}
