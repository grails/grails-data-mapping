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
package org.grails.datastore.mapping.jpa;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceException;

import org.springframework.core.convert.ConversionService;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.grails.datastore.mapping.core.AbstractAttributeStoringSession;
import org.grails.datastore.mapping.engine.EntityAccess;
import org.grails.datastore.mapping.engine.Persister;
import org.grails.datastore.mapping.jpa.query.JpaQuery;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.query.Query;
import org.grails.datastore.mapping.transactions.Transaction;
import org.springframework.orm.jpa.JpaCallback;
import org.springframework.orm.jpa.JpaTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * Wraps a JPA EntityManager in the Datastore Session interface
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class JpaSession extends AbstractAttributeStoringSession {

    private JpaDatastore datastore;
    private JpaTemplate jpaTemplate;
    private JpaTransactionManager transactionManager;
    private FlushModeType flushMode;
    private TransactionStatus transaction;

    public JpaSession(JpaDatastore datastore, JpaTemplate jpaTemplate, JpaTransactionManager transactionManager) {
        this.jpaTemplate = jpaTemplate;
        this.datastore = datastore;
        this.transactionManager = transactionManager;
    }

    public JpaTemplate getJpaTemplate() {
        return jpaTemplate;
    }

    public Transaction beginTransaction() {
        final TransactionStatus status = transactionManager.getTransaction(
                new DefaultTransactionDefinition());
        return new JpaTransaction(transactionManager, status);
    }

    public MappingContext getMappingContext() {
        return datastore.getMappingContext();
    }

    public Serializable persist(Object o) {
        if (o == null) {
            throw new InvalidDataAccessApiUsageException("Object to persist cannot be null");
        }

        final PersistentEntity persistentEntity = getMappingContext().getPersistentEntity(o.getClass().getName());
        if (persistentEntity == null) {
            throw new InvalidDataAccessApiUsageException("Object of class [" +
                    o.getClass().getName() + "] is not a persistent entity");
        }

        jpaTemplate.persist(o);
        return (Serializable)new EntityAccess(persistentEntity, o).getIdentifier();
    }

    public Object merge(Object o) {
        if (o == null) {
            throw new InvalidDataAccessApiUsageException("Object to merge cannot be null");
        }

        final PersistentEntity persistentEntity = getMappingContext().getPersistentEntity(o.getClass().getName());
        if (persistentEntity == null) {
            throw new InvalidDataAccessApiUsageException("Object of class [" +
                    o.getClass().getName() + "] is not a persistent entity");
        }

        return jpaTemplate.merge(o);
    }

    public void refresh(Object o) {
        if (o == null) {
            return;
        }
        jpaTemplate.refresh(o);
    }

    public void attach(Object o) {
        if (o == null) {
            return;
        }
        jpaTemplate.merge(o);
    }

    public void flush() {
        jpaTemplate.flush();
    }

    public void clear() {
        jpaTemplate.execute(new JpaCallback<Void>() {
            public Void doInJpa(EntityManager em) throws PersistenceException {
                em.clear();
                return null;
            }
        });
    }

    public void clear(Object o) {
        // do nothing
    }

    public boolean contains(Object o) {
        return jpaTemplate.contains(o);
    }

    public void setFlushMode(FlushModeType flushMode) {
        this.flushMode = flushMode;
        jpaTemplate.setFlushEager(flushMode == FlushModeType.AUTO);
    }

    public FlushModeType getFlushMode() {
        return flushMode;
    }

    public void lock(final Object o) {
        jpaTemplate.execute(new JpaCallback<Void>() {
            public Void doInJpa(EntityManager em) throws PersistenceException {
                em.lock(o, LockModeType.WRITE);
                return null;
            }
        });
    }

    public void unlock(Object o) {
        // noop. Not supported in JPA
    }

    public List<Serializable> persist(final Iterable objects) {
        return jpaTemplate.execute(new JpaCallback<List<Serializable>>() {
            public List<Serializable> doInJpa(EntityManager em) throws PersistenceException {
                List<Serializable> identifiers = new ArrayList<Serializable>();
                for (Object object : objects) {
                    identifiers.add(persist(object));
                }
                return identifiers;
            }
        });
    }

    public <T> T retrieve(Class<T> type, Serializable key) {
        final PersistentEntity persistentEntity = getPersistentEntity(type);
        if (persistentEntity == null) {
            return null;
        }

        final ConversionService conversionService = getMappingContext().getConversionService();
        final Object id = conversionService.convert(key, persistentEntity.getIdentity().getType());
        return jpaTemplate.find(type, id);
    }

    public <T> T proxy(Class<T> type, Serializable key) {
        return jpaTemplate.getReference(type, key);
    }

    public <T> T lock(Class<T> type, Serializable key) {
        final T obj = retrieve(type, key);
        lock(obj);
        return obj;
    }

    public void delete(Iterable objects) {
        for (Object object : objects) {
            jpaTemplate.remove(object);
        }
    }

    public void delete(Object obj) {
        jpaTemplate.remove(obj);
    }

    public List retrieveAll(Class type, Iterable keys) {
        if (keys instanceof List) {
            return retrieveAll(getPersistentEntity(type), (List)keys);
        }
        List identifierList = new ArrayList();
        for (Object key : keys) {
            identifierList.add(key);
        }
        return retrieveAll(getPersistentEntity(type), identifierList);
    }

    public PersistentEntity getPersistentEntity(Class type) {
        return getMappingContext().getPersistentEntity(type.getName());
    }

    public List retrieveAll(Class type, Serializable... keys) {
        if (type == null) {
            return Collections.emptyList();
        }

        final PersistentEntity persistentEntity = getPersistentEntity(type);
        if (persistentEntity == null) {
            return Collections.emptyList();
        }

        final List<Serializable> identifiers = Arrays.asList(keys);
        return retrieveAll(persistentEntity, identifiers);
    }

    public List retrieveAll(final PersistentEntity persistentEntity, final List<Serializable> identifiers) {
        return createQuery(persistentEntity.getJavaClass())
            .in(persistentEntity.getIdentity().getName(), identifiers)
            .list();
    }

    public Query createQuery(Class type) {
        return new JpaQuery(this, getPersistentEntity(type));
    }

    public Object getNativeInterface() {
        return jpaTemplate;
    }

    public Persister getPersister(Object o) {
        return null;
    }

    public Transaction getTransaction() {
        if (transaction == null) {
            return null;
        }
        return new JpaTransaction(transactionManager, transaction);
    }

    public JpaDatastore getDatastore() {
        return datastore;
    }

    public void setTransactionStatus(TransactionStatus transaction) {
        this.transaction = transaction;
    }

    public boolean isDirty(Object instance) {
        // TODO
        return false;
    }
}
