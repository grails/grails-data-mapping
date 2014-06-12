/* Copyright (C) 2011 SpringSource
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
package org.grails.datastore.gorm.jpa.support;

import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;

import org.codehaus.groovy.grails.support.PersistenceContextInterceptor;
import org.grails.datastore.mapping.jpa.JpaDatastore;
import org.grails.datastore.mapping.jpa.JpaSession;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;

/**
 * @author Graeme Rocher
 * @since 1.0
 */
public class JpaPersistenceContextInterceptor implements PersistenceContextInterceptor {

    private JpaDatastore jpaDatastore;
    private EntityManager entityManager;

    public JpaPersistenceContextInterceptor(JpaDatastore datastore) {
        this.jpaDatastore= datastore;
    }

    public void init() {
        entityManager = EntityManagerFactoryUtils.getTransactionalEntityManager(jpaDatastore.getEntityManagerFactory());
    }

    public void destroy() {
        entityManager = null;
    }

    public void disconnect() {
        if (entityManager != null) {
            EntityManagerFactoryUtils.closeEntityManager(entityManager);
        }
    }

    public void reconnect() {
        entityManager = EntityManagerFactoryUtils.getTransactionalEntityManager(jpaDatastore.getEntityManagerFactory());
    }

    public void flush() {
        if (JpaSession.hasTransaction()) {
            entityManager.flush();
        }
    }

    public void clear() {
        entityManager.clear();
    }

    public void setReadOnly() {
        entityManager.setFlushMode(FlushModeType.COMMIT);
    }

    public void setReadWrite() {
        entityManager.setFlushMode(FlushModeType.AUTO);
    }

    public boolean isOpen() {
        return entityManager != null && entityManager.isOpen();
    }
}
