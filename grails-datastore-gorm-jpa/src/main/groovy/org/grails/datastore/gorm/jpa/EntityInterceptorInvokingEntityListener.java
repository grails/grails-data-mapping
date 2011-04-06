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
package org.grails.datastore.gorm.jpa;

import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

import org.grails.datastore.gorm.events.DomainEventListener;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.datastore.mapping.core.AbstractDatastore;
import org.springframework.datastore.mapping.core.ConnectionNotFoundException;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.engine.EntityAccess;
import org.springframework.datastore.mapping.engine.event.AbstractPersistenceEvent;
import org.springframework.datastore.mapping.engine.event.PostDeleteEvent;
import org.springframework.datastore.mapping.engine.event.PostInsertEvent;
import org.springframework.datastore.mapping.engine.event.PostLoadEvent;
import org.springframework.datastore.mapping.engine.event.PostUpdateEvent;
import org.springframework.datastore.mapping.engine.event.PreDeleteEvent;
import org.springframework.datastore.mapping.engine.event.PreInsertEvent;
import org.springframework.datastore.mapping.engine.event.PreUpdateEvent;
import org.springframework.datastore.mapping.jpa.JpaDatastore;
import org.springframework.datastore.mapping.jpa.JpaSession;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.transactions.Transaction;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

/**
 * Adapts JPA events to the Datastore abstraction event API
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class EntityInterceptorInvokingEntityListener {

    @PrePersist
    public void prePersist(final Object o) {
        doWithSession(o, new JpaSessionTemplate() {
            public void doWithSession(final JpaSession session, final PersistentEntity entity, final EntityAccess ea) {
                PreInsertEvent event = new PreInsertEvent(session.getDatastore(), entity, ea);
                event.addExcludedListenerName(DomainEventListener.class.getName());
                publishEvent(session, event);
                if (event.isCancelled()) {
                    rollbackTransaction(session);
                }
            }
        });
    }

    @PreUpdate
    public void preUpdate(final Object o) {
        doWithSession(o, new JpaSessionTemplate() {
            public void doWithSession(final JpaSession session, final PersistentEntity entity, final EntityAccess ea) {
                PreUpdateEvent event = new PreUpdateEvent(session.getDatastore(), entity, ea);
                event.addExcludedListenerName(DomainEventListener.class.getName());
                session.getDatastore().getApplicationEventPublisher().publishEvent(event);
                if (event.isCancelled()) {
                    rollbackTransaction(session);
                }
            }
        });
    }

    @PreRemove
    public void preRemove(final Object o) {
        doWithSession(o, new JpaSessionTemplate() {
            public void doWithSession(final JpaSession session, final PersistentEntity entity, final EntityAccess ea) {
                PreDeleteEvent event = new PreDeleteEvent(session.getDatastore(), entity, ea);
                event.addExcludedListenerName(DomainEventListener.class.getName());
                session.getDatastore().getApplicationEventPublisher().publishEvent(event);
                if (event.isCancelled()) {
                    rollbackTransaction(session);
                }
            }
        });
    }

    @PostPersist
    public void postPersist(final Object o) {
        doWithSession(o, new JpaSessionTemplate() {
            public void doWithSession(final JpaSession session, final PersistentEntity entity, final EntityAccess ea) {
                PostInsertEvent event = new PostInsertEvent(session.getDatastore(), entity, ea);
                event.addExcludedListenerName(DomainEventListener.class.getName());
                session.getDatastore().getApplicationEventPublisher().publishEvent(event);
            }
        });
    }

    @PostUpdate
    public void postUpdate(final Object o) {
        doWithSession(o, new JpaSessionTemplate() {
            public void doWithSession(final JpaSession session, final PersistentEntity entity, final EntityAccess ea) {
                PostUpdateEvent event = new PostUpdateEvent(session.getDatastore(), entity, ea);
                event.addExcludedListenerName(DomainEventListener.class.getName());
                session.getDatastore().getApplicationEventPublisher().publishEvent(event);
            }
        });
    }

    @PostRemove
    public void postRemove(final Object o) {
        doWithSession(o, new JpaSessionTemplate() {
            public void doWithSession(final JpaSession session, final PersistentEntity entity, final EntityAccess ea) {
                PostDeleteEvent event = new PostDeleteEvent(session.getDatastore(), entity, ea);
                event.addExcludedListenerName(DomainEventListener.class.getName());
                session.getDatastore().getApplicationEventPublisher().publishEvent(event);
            }
        });
    }

    @PostLoad
    public void postLoad(final Object o) {
        doWithSession(o, new JpaSessionTemplate() {
            public void doWithSession(final JpaSession session, final PersistentEntity entity, final EntityAccess ea) {

                session.getDatastore().getApplicationContext().getAutowireCapableBeanFactory().autowireBeanProperties(
                      o, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);

                PostLoadEvent event = new PostLoadEvent(session.getDatastore(), entity, ea);
                event.addExcludedListenerName(DomainEventListener.class.getName());
                session.getDatastore().getApplicationEventPublisher().publishEvent(event);
            }
        });
    }

    void rollbackTransaction(JpaSession jpaSession) {
        final Transaction transaction = jpaSession.getTransaction();
        if (transaction != null) {
            transaction.rollback();
        }
        else {
            try {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            }
            catch (NoTransactionException e) {
                // ignore
            }
        }
    }

    private void doWithSession(Object o, JpaSessionTemplate template) {
        try {
            final Session session = AbstractDatastore.retrieveSession(JpaDatastore.class);
            if (!(session instanceof JpaSession)) {
                return;
            }

            JpaSession jpaSession = (JpaSession)session;
            final PersistentEntity entity = session.getMappingContext().getPersistentEntity(o.getClass().getName());
            if (entity == null) {
                return;
            }

            template.doWithSession(jpaSession, entity, new EntityAccess(entity, o));
        }
        catch (ConnectionNotFoundException e) {
            // ignore, shouldn't happen
        }
    }

    private void publishEvent(final JpaSession session, final AbstractPersistenceEvent event) {
        session.getDatastore().getApplicationEventPublisher().publishEvent(event);
    }

    private static interface JpaSessionTemplate {
        void doWithSession(JpaSession session, PersistentEntity entity, EntityAccess entityAccess);
    }
}
