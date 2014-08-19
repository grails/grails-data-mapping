/*
 * Copyright 2013 the original author or authors.
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
package org.codehaus.groovy.grails.orm.hibernate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.context.spi.CurrentSessionContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.service.spi.ServiceBinding;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.orm.hibernate4.SessionHolder;
import org.springframework.orm.hibernate4.SpringJtaSessionContext;
import org.springframework.transaction.jta.SpringJtaSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.ReflectionUtils;

import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Based on org.springframework.orm.hibernate4.SpringSessionContext.
 *
 * @author Juergen Hoeller
 * @author Burt Beckwith
 */
public class GrailsSessionContext implements CurrentSessionContext {

    private static final long serialVersionUID = 1;

    private static final Log LOG = LogFactory.getLog(GrailsSessionContext.class);

    protected final SessionFactoryImplementor sessionFactory;
    protected CurrentSessionContext jtaSessionContext;
    protected Constructor<?> springFlushSynchronizationConstructor;
    protected Constructor<?> springSessionSynchronizationConstructor;

    // TODO make configurable?
    protected boolean allowCreate = true;

    /**
     * Constructor.
     * @param sessionFactory the SessionFactory to provide current Sessions for
     */
    public GrailsSessionContext(SessionFactoryImplementor sessionFactory) {
        this.sessionFactory = sessionFactory;
        lookupConstructors();
    }

    public void initJta() {
        JtaPlatform jtaPlatform = sessionFactory.getServiceRegistry().getService(JtaPlatform.class);
        TransactionManager transactionManager = jtaPlatform.retrieveTransactionManager();
        jtaSessionContext = transactionManager == null ? null : new SpringJtaSessionContext(sessionFactory);
    }

    /**
     * Retrieve the Spring-managed Session for the current thread, if any.
     */
    public Session currentSession() throws HibernateException {
        Object value = TransactionSynchronizationManager.getResource(sessionFactory);
        if (value instanceof Session) {
            return (Session) value;
        }

        if (value instanceof SessionHolder) {
            SessionHolder sessionHolder = (SessionHolder) value;
            Session session = sessionHolder.getSession();
            if (TransactionSynchronizationManager.isSynchronizationActive() && !sessionHolder.isSynchronizedWithTransaction()) {
                TransactionSynchronizationManager.registerSynchronization(createSpringSessionSynchronization(sessionHolder));
                sessionHolder.setSynchronizedWithTransaction(true);
                // Switch to FlushMode.AUTO, as we have to assume a thread-bound Session
                // with FlushMode.MANUAL, which needs to allow flushing within the transaction.
                FlushMode flushMode = session.getFlushMode();
                if (FlushMode.isManualFlushMode(flushMode) && !TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
                    session.setFlushMode(FlushMode.AUTO);
                    sessionHolder.setPreviousFlushMode(flushMode);
                }
            }
            return session;
        }

        if (jtaSessionContext != null) {
            Session session = jtaSessionContext.currentSession();
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(createSpringFlushSynchronization(session));
            }
            return session;
        }

        if (allowCreate) {
            // be consistent with older HibernateTemplate behavior
            return createSession(value);
        }

        throw new HibernateException("No Session found for current thread");
    }

    private Session createSession(Object resource) {
        LOG.debug("Opening Hibernate Session");

        SessionHolder sessionHolder = (SessionHolder) resource;

        Session session = sessionFactory.openSession();

        // Use same Session for further Hibernate actions within the transaction.
        // Thread object will get removed by synchronization at transaction completion.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
           // We're within a Spring-managed transaction, possibly from JtaTransactionManager.
           LOG.debug("Registering Spring transaction synchronization for new Hibernate Session");
           SessionHolder holderToUse = sessionHolder;
           if (holderToUse == null) {
              holderToUse = new SessionHolder(session);
           }
           else {
               // it's up to the caller to manage concurrent sessions
               // holderToUse.addSession(session);
           }
           if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
              session.setFlushMode(FlushMode.MANUAL);
           }
           TransactionSynchronizationManager.registerSynchronization(createSpringSessionSynchronization(holderToUse));
           holderToUse.setSynchronizedWithTransaction(true);
           if (holderToUse != sessionHolder) {
              TransactionSynchronizationManager.bindResource(sessionFactory, holderToUse);
           }
        }
        else {
           // No Spring transaction management active -> try JTA transaction synchronization.
           registerJtaSynchronization(session, sessionHolder);
        }

/*        // Check whether we are allowed to return the Session.
        if (!allowCreate && !isSessionTransactional(session, sessionFactory)) {
           closeSession(session);
           throw new IllegalStateException("No Hibernate Session bound to thread, " +
              "and configuration does not allow creation of non-transactional one here");
        }
*/
        return session;
    }

    protected void registerJtaSynchronization(Session session, SessionHolder sessionHolder) {

        // JTA synchronization is only possible with a javax.transaction.TransactionManager.
        // We'll check the Hibernate SessionFactory: If a TransactionManagerLookup is specified
        // in Hibernate configuration, it will contain a TransactionManager reference.
        TransactionManager jtaTm = getJtaTransactionManager(session);
        if (jtaTm == null) {
            return;
        }

        try {
            Transaction jtaTx = jtaTm.getTransaction();
            if (jtaTx == null) {
                return;
            }

            int jtaStatus = jtaTx.getStatus();
            if (jtaStatus != Status.STATUS_ACTIVE && jtaStatus != Status.STATUS_MARKED_ROLLBACK) {
                return;
            }

            LOG.debug("Registering JTA transaction synchronization for new Hibernate Session");
            SessionHolder holderToUse = sessionHolder;
            // Register JTA Transaction with existing SessionHolder.
            // Create a new SessionHolder if none existed before.
            if (holderToUse == null) {
                holderToUse = new SessionHolder(session);
            }
            else {
                // it's up to the caller to manage concurrent sessions
                // holderToUse.addSession(session);
            }
            jtaTx.registerSynchronization(new SpringJtaSynchronizationAdapter(createSpringSessionSynchronization(holderToUse), jtaTm));
            holderToUse.setSynchronizedWithTransaction(true);
            if (holderToUse != sessionHolder) {
                TransactionSynchronizationManager.bindResource(sessionFactory, holderToUse);
            }
        }
        catch (Throwable ex) {
            throw new DataAccessResourceFailureException("Could not register synchronization with JTA TransactionManager", ex);
        }
    }

    protected TransactionManager getJtaTransactionManager(Session session) {
        SessionFactoryImplementor sessionFactoryImpl = null;
        if (sessionFactory instanceof SessionFactoryImplementor) {
            sessionFactoryImpl = ((SessionFactoryImplementor) sessionFactory);
        }
        else if (session != null) {
            SessionFactory internalFactory = session.getSessionFactory();
            if (internalFactory instanceof SessionFactoryImplementor) {
                sessionFactoryImpl = (SessionFactoryImplementor) internalFactory;
            }
        }

        if (sessionFactoryImpl == null) {
            return null;
        }

        ServiceBinding<JtaPlatform> sb = sessionFactory.getServiceRegistry().locateServiceBinding(JtaPlatform.class);
        if (sb == null) {
            return null;
        }

        return sb.getService().retrieveTransactionManager();
    }

    protected void lookupConstructors() {
        springFlushSynchronizationConstructor = lookupConstructor(
                "org.springframework.orm.hibernate4.SpringFlushSynchronization", Session.class);
        try{
            springSessionSynchronizationConstructor = lookupConstructor(
                    "org.springframework.orm.hibernate4.SpringSessionSynchronization", SessionHolder.class, SessionFactory.class);
        }catch(Exception e){
            springSessionSynchronizationConstructor = lookupConstructor(
                    "org.springframework.orm.hibernate4.SpringSessionSynchronization", SessionHolder.class, SessionFactory.class, int.class);
        }
    }

    protected Constructor<?> lookupConstructor(String className, Class<?>... argTypes) {
        try {
            Class<?> clazz = ReflectHelper.classForName(className);
            Constructor<?> constructor = clazz.getConstructor(argTypes);
            constructor.setAccessible(true);
            return constructor;
        }
        catch (ClassNotFoundException e) {
            ReflectionUtils.handleReflectionException(e);
        }
        catch (NoSuchMethodException e) {
            ReflectionUtils.handleReflectionException(e);
        }
        catch (SecurityException e) {
            ReflectionUtils.handleReflectionException(e);
        }
        return null;
    }

    protected TransactionSynchronization createSpringFlushSynchronization(Session session) {
        return (TransactionSynchronization)create(springFlushSynchronizationConstructor, session);
    }

    protected TransactionSynchronization createSpringSessionSynchronization(SessionHolder sessionHolder) {
        if(springSessionSynchronizationConstructor.getTypeParameters().length==3)
            return (TransactionSynchronization)create(springSessionSynchronizationConstructor, sessionHolder, sessionFactory);
        else
            return (TransactionSynchronization)create(springSessionSynchronizationConstructor, sessionHolder, sessionFactory, true);
    }

    protected Object create(Constructor<?> constructor, Object... args) {
        try {
            return constructor.newInstance(args);
        }
        catch (InstantiationException e) {
            ReflectionUtils.handleReflectionException(e);
        }
        catch (IllegalAccessException e) {
            ReflectionUtils.handleReflectionException(e);
        }
        catch (IllegalArgumentException e) {
            ReflectionUtils.handleReflectionException(e);
        }
        catch (InvocationTargetException e) {
            ReflectionUtils.handleInvocationTargetException(e);
        }
        return null;
    }
}
