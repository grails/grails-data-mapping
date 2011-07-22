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
package org.grails.datastore.mapping.jcr;

import java.io.Serializable;
import java.util.Date;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.core.TransientRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.convert.converter.Converter;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.grails.datastore.mapping.core.AbstractSession;
import org.grails.datastore.mapping.core.Datastore;
import org.grails.datastore.mapping.engine.LockableEntityPersister;
import org.grails.datastore.mapping.engine.Persister;
import org.grails.datastore.mapping.jcr.engine.JcrEntityPersister;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.transactions.Transaction;
import org.springframework.extensions.jcr.JcrSessionFactory;
import org.springframework.extensions.jcr.JcrTemplate;
import org.springframework.extensions.jcr.support.OpenSessionInViewInterceptor;

/**
 * @author Erawat Chamanont
 * @since 1.0
 */
public class JcrSession extends AbstractSession<JcrSessionFactory> {

    protected OpenSessionInViewInterceptor interceptor = new OpenSessionInViewInterceptor();

    private JcrSessionFactory jcrSessionFactory;

    public JcrSession(Datastore ds, MappingContext mappingContext, JcrSessionFactory jcrSessionFactory,
              ApplicationEventPublisher publisher) {
        super(ds, mappingContext, publisher);
        mappingContext.addTypeConverter(new LongToDateConverter());
        this.jcrSessionFactory = jcrSessionFactory;
        interceptor.setSessionFactory(jcrSessionFactory);
        interceptor.preHandle(null, null, null);
    }

    @Override
    protected Persister createPersister(Class cls, MappingContext mappingContext) {
        PersistentEntity entity = mappingContext.getPersistentEntity(cls.getName());
        if (entity == null) {
            return null;
        }
        return new JcrEntityPersister(mappingContext, entity, this,
                 new JcrTemplate(jcrSessionFactory), publisher);
    }

    @Override
    public void disconnect() {
//        interceptor.afterCompletion(null, null, null, null);
        try {
            ((TransientRepository) jcrSessionFactory.getRepository()).shutdown();
            super.disconnect();
        } catch (Exception e) {
            throw new DataAccessResourceFailureException("Failed to disconnect JCR Repository: " + e.getMessage(), e);
        } finally {
            super.disconnect();
        }
    }

    @Override
    public boolean isConnected() {
        try {
            return jcrSessionFactory.getSession().isLive();
        } catch (RepositoryException e) {
            throw new DataAccessResourceFailureException("Repository Errors: " + e.getMessage(), e);
        }
    }

    @Override
    protected Transaction beginTransactionInternal() {
        return new JcrTransaction(jcrSessionFactory);
    }

    public Session getNativeInterface() {
        try {
            return jcrSessionFactory.getSession();
        } catch (RepositoryException e) {
            throw new DataAccessResourceFailureException("Session not found: " + e.getMessage(), e);
        }
    }

    @Override
    public void lock(Object o) {
        LockableEntityPersister ep = (LockableEntityPersister) getPersister(o);
        if (ep == null) {
            throw new CannotAcquireLockException("Cannot lock object [" + o +
                "]. It is not a persistent instance!");
        }

        Serializable id = ep.getObjectIdentifier(o);
        if (id == null) {
            throw new CannotAcquireLockException("Cannot lock transient instance [" + o + "]");
        }

        ep.lock(id);
    }

    @Override
    public void unlock(Object o) {
        if (o == null) {
            return;
        }

        LockableEntityPersister ep = (LockableEntityPersister) getPersister(o);
        if (ep == null) {
            return;
        }

        ep.unlock(o);
        lockedObjects.remove(o);
    }

    @Override
    public Object lock(Class type, Serializable key) {
        LockableEntityPersister ep = (LockableEntityPersister) getPersister(type);
        if (ep == null) {
            throw new CannotAcquireLockException("Cannot lock key [" + key +
                "]. It is not a persistent instance!");
        }

        final Object lockedObject = ep.lock(key);
        if (lockedObject != null) {
            cacheObject(key, lockedObject);
            lockedObjects.add(lockedObject);
        }
        return lockedObject;
    }

    protected class LongToDateConverter implements Converter<Long, Date> {
        public Date convert(Long number) {
            return new Date(number);
        }
    }
}
