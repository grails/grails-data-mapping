/*
 * Copyright 2011-2013 SpringSource.
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

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.exception.GenericJDBCException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.orm.hibernate4.SessionFactoryUtils;
import org.springframework.orm.hibernate4.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

public class GrailsHibernateTemplate {

    protected final Log logger = LogFactory.getLog(getClass());

    private boolean osivReadOnly;
    protected boolean exposeNativeSession = true;
    protected boolean cacheQueries = false;

protected boolean allowCreate = true;
protected boolean checkWriteOperations = true;

    protected SessionFactory sessionFactory;
    protected SQLExceptionTranslator jdbcExceptionTranslator;
    protected int flushMode = FLUSH_AUTO;

    public static interface HibernateCallback<T> {
       T doInHibernate(Session session) throws HibernateException, SQLException;
    }

    protected GrailsHibernateTemplate() {
        // for testing
    }
    
    public GrailsHibernateTemplate(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public GrailsHibernateTemplate(SessionFactory sessionFactory, GrailsApplication application) {
        Assert.notNull(sessionFactory, "Property 'sessionFactory' is required");
        this.sessionFactory = sessionFactory;

        DataSource ds = application.getMainContext().getBean("dataSource", DataSource.class);
        jdbcExceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(ds);

        cacheQueries = GrailsHibernateUtil.isCacheQueriesByDefault(application);
        this.osivReadOnly = GrailsHibernateUtil.isOsivReadonly(application);
    }

    public void applySettings(Query query) {
        if (exposeNativeSession) {
            prepareQuery(query);
        }
    }

    public void applySettings(Criteria criteria) {
        if (exposeNativeSession) {
            prepareCriteria(criteria);
        }
    }

    public void setCacheQueries(boolean cacheQueries) {
        this.cacheQueries = cacheQueries;
    }
    public boolean isCacheQueries() {
        return cacheQueries;
    }

    public <T> T execute(HibernateCallback<T> action) throws DataAccessException {
        return doExecute(action, false);
    }

    public List<?> executeFind(HibernateCallback<?> action) throws DataAccessException {
        Object result = doExecute(action, false);
        if (result != null && !(result instanceof List)) {
            throw new InvalidDataAccessApiUsageException("Result object returned from HibernateCallback isn't a List: [" + result + "]");
        }
        return (List<?>)result;
    }

    protected boolean isCurrentTransactionReadOnly() {
        if(TransactionSynchronizationManager.hasResource(sessionFactory)) {
            if(TransactionSynchronizationManager.isActualTransactionActive()) {
                return TransactionSynchronizationManager.isCurrentTransactionReadOnly();
            } else {
                return osivReadOnly;
            }
        } else {
            return false;
        }
    }

    public boolean isOsivReadOnly() {
        return osivReadOnly;
    }

    public void setOsivReadOnly(boolean osivReadOnly) {
        this.osivReadOnly = osivReadOnly;
    }

    /**
     * Execute the action specified by the given action object within a Session.
     *
     * @param action callback object that specifies the Hibernate action
     * @param enforceNativeSession whether to enforce exposure of the native Hibernate Session to callback code
     * @return a result object returned by the action, or <code>null</code>
     * @throws org.springframework.dao.DataAccessException in case of Hibernate errors
     */
    protected <T> T doExecute(HibernateCallback<T> action, boolean enforceNativeSession) throws DataAccessException {

        Assert.notNull(action, "Callback object must not be null");

        Session session = getSession();
        boolean existingTransaction = isSessionTransactional(session);
        if (existingTransaction) {
            logger.debug("Found thread-bound Session for HibernateTemplate");
        }

        FlushMode previousFlushMode = null;
        try {
            previousFlushMode = applyFlushMode(session, existingTransaction);
			if(isCurrentTransactionReadOnly()) {
            	session.setDefaultReadOnly(true);
	        }
            Session sessionToExpose = (enforceNativeSession || exposeNativeSession ? session : createSessionProxy(session));
            T result = action.doInHibernate(sessionToExpose);
            flushIfNecessary(session, existingTransaction);
            return result;
        }
        catch (HibernateException ex) {
            throw convertHibernateAccessException(ex);
        }
        catch (SQLException ex) {
            throw jdbcExceptionTranslator.translate("Hibernate-related JDBC operation", null, ex);
        }
        catch (RuntimeException ex) {
            // Callback code threw application exception...
            throw ex;
        }
        finally {
            if (existingTransaction) {
                logger.debug("Not closing pre-bound Hibernate Session after HibernateTemplate");
                if (previousFlushMode != null) {
                    session.setFlushMode(previousFlushMode);
                }
            }
            else {
                SessionFactoryUtils.closeSession(session);
            }
        }
    }

    protected boolean isSessionTransactional(Session session) {
        SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.getResource(sessionFactory);
        return sessionHolder != null && sessionHolder.getSession() == session;
    }

    protected Session getSession() {
        try {
            return sessionFactory.getCurrentSession();
        }
        catch (HibernateException ex) {
            throw new DataAccessResourceFailureException("Could not obtain current Hibernate Session", ex);
        }
    }

    /**
     * Create a close-suppressing proxy for the given Hibernate Session. The
     * proxy also prepares returned Query and Criteria objects.
     *
     * @param session the Hibernate Session to create a proxy for
     * @return the Session proxy
     * @see org.hibernate.Session#close()
     * @see #prepareQuery
     * @see #prepareCriteria
     */
    protected Session createSessionProxy(Session session) {
        Class<?>[] sessionIfcs = null;
        Class<?> mainIfc = Session.class;
        if (session instanceof EventSource) {
            sessionIfcs = new Class[] { mainIfc, EventSource.class };
        }
        else if (session instanceof SessionImplementor) {
            sessionIfcs = new Class[] { mainIfc, SessionImplementor.class };
        }
        else {
            sessionIfcs = new Class[] { mainIfc };
        }
        return (Session) Proxy.newProxyInstance(session.getClass().getClassLoader(), sessionIfcs,
                new CloseSuppressingInvocationHandler(session));
    }

    public <T> T get(final Class<T> entityClass, final Serializable id) throws DataAccessException {
        return doExecute(new HibernateCallback<T>() {
            @SuppressWarnings("unchecked")
            public T doInHibernate(Session session) throws HibernateException {
                return (T) session.get(entityClass, id);
            }
        }, true);
    }

    public void delete(final Object entity) throws DataAccessException {
        doExecute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException {
                session.delete(entity);
                return null;
            }
        }, true);
    }

    public void flush(final Object entity) throws DataAccessException {
        doExecute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException {
                session.flush();
                return null;
            }
        }, true);
    }

    public <T> T load(final Class<T> entityClass, final Serializable id) throws DataAccessException {
        return doExecute(new HibernateCallback<T>() {
            @SuppressWarnings("unchecked")
            public T doInHibernate(Session session) throws HibernateException {
                return (T) session.load(entityClass, id);
            }
        }, true);
    }

    public <T> T lock(final Class<T> entityClass, final Serializable id, final LockMode lockMode) throws DataAccessException {
        return doExecute(new HibernateCallback<T>() {
            @SuppressWarnings("unchecked")
            public T doInHibernate(Session session) throws HibernateException {
                return (T) session.get(entityClass, id, new LockOptions(lockMode));
            }
        }, true);
    }

    public <T> List<T> loadAll(final Class<T> entityClass) throws DataAccessException {
        return doExecute(new HibernateCallback<List<T>>() {
            @SuppressWarnings("unchecked")
            public List<T> doInHibernate(Session session) throws HibernateException {
                Criteria criteria = session.createCriteria(entityClass);
                criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
                prepareCriteria(criteria);
                return criteria.list();
            }
        }, true);
    }

    public boolean contains(final Object entity) throws DataAccessException {
        return doExecute(new HibernateCallback<Boolean>() {
            public Boolean doInHibernate(Session session) {
                return session.contains(entity);
            }
        }, true);
    }

    public void evict(final Object entity) throws DataAccessException {
        doExecute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException {
                session.evict(entity);
                return null;
            }
        }, true);
    }

    public void lock(final Object entity, final LockMode lockMode) throws DataAccessException {
        doExecute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException {
                session.buildLockRequest(new LockOptions(lockMode)).lock(entity);//LockMode.PESSIMISTIC_WRITE
                return null;
            }
        }, true);
    }

    public void refresh(final Object entity) throws DataAccessException {
        refresh(entity, null);
    }

    public void refresh(final Object entity, final LockMode lockMode) throws DataAccessException {
        doExecute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException {
                if (lockMode == null) {
                    session.refresh(entity);
                }
                else {
                    session.refresh(entity, new LockOptions(lockMode));
                }
                return null;
            }
        }, true);
    }

    public void setExposeNativeSession(boolean exposeNativeSession) {
        this.exposeNativeSession = exposeNativeSession;
    }
    public boolean isExposeNativeSession() {
        return exposeNativeSession;
    }

    /**
     * Prepare the given Query object, applying cache settings and/or a
     * transaction timeout.
     *
     * @param query the Query object to prepare
     * @see SessionFactoryUtils#applyTransactionTimeout
     */
    protected void prepareQuery(Query query) {
        if (cacheQueries) {
            query.setCacheable(true);
        }
        if(isCurrentTransactionReadOnly()) {
            query.setReadOnly(true);
        }
        SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.getResource(sessionFactory);
        if (sessionHolder != null && sessionHolder.hasTimeout()) {
            query.setTimeout(sessionHolder.getTimeToLiveInSeconds());
        }
    }

    /**
     * Prepare the given Criteria object, applying cache settings and/or a
     * transaction timeout.
     *
     * @param criteria the Criteria object to prepare
     * @see SessionFactoryUtils#applyTransactionTimeout
     */
    protected void prepareCriteria(Criteria criteria) {
        if (cacheQueries) {
            criteria.setCacheable(true);
        }
        if(isCurrentTransactionReadOnly()) {
            criteria.setReadOnly(true);
        }
        SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.getResource(sessionFactory);
        if (sessionHolder != null && sessionHolder.hasTimeout()) {
            criteria.setTimeout(sessionHolder.getTimeToLiveInSeconds());
        }
    }

    /**
     * Invocation handler that suppresses close calls on Hibernate Sessions.
     * Also prepares returned Query and Criteria objects.
     *
     * @see org.hibernate.Session#close
     */
    protected class CloseSuppressingInvocationHandler implements InvocationHandler {

        protected final Session target;

        protected CloseSuppressingInvocationHandler(Session target) {
            this.target = target;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // Invocation on Session interface coming in...

            if (method.getName().equals("equals")) {
                // Only consider equal when proxies are identical.
                return (proxy == args[0]);
            }
            if (method.getName().equals("hashCode")) {
                // Use hashCode of Session proxy.
                return System.identityHashCode(proxy);
            }
            if (method.getName().equals("close")) {
                // Handle close method: suppress, not valid.
                return null;
            }

            // Invoke method on target Session.
            try {
                Object retVal = method.invoke(target, args);

                // If return value is a Query or Criteria, apply transaction timeout.
                // Applies to createQuery, getNamedQuery, createCriteria.
                if (retVal instanceof Query) {
                    prepareQuery(((Query) retVal));
                }
                if (retVal instanceof Criteria) {
                    prepareCriteria(((Criteria) retVal));
                }

                return retVal;
            }
            catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }
        }
    }

    /**
     * Never flush is a good strategy for read-only units of work.
     * Hibernate will not track and look for changes in this case,
     * avoiding any overhead of modification detection.
     * <p>In case of an existing Session, FLUSH_NEVER will turn the flush mode
     * to NEVER for the scope of the current operation, resetting the previous
     * flush mode afterwards.
     * @see #setFlushMode
     */
    public static final int FLUSH_NEVER = 0;

    /**
     * Automatic flushing is the default mode for a Hibernate Session.
     * A session will get flushed on transaction commit, and on certain find
     * operations that might involve already modified instances, but not
     * after each unit of work like with eager flushing.
     * <p>In case of an existing Session, FLUSH_AUTO will participate in the
     * existing flush mode, not modifying it for the current operation.
     * This in particular means that this setting will not modify an existing
     * flush mode NEVER, in contrast to FLUSH_EAGER.
     * @see #setFlushMode
     */
    public static final int FLUSH_AUTO = 1;

    /**
     * Eager flushing leads to immediate synchronization with the database,
     * even if in a transaction. This causes inconsistencies to show up and throw
     * a respective exception immediately, and JDBC access code that participates
     * in the same transaction will see the changes as the database is already
     * aware of them then. But the drawbacks are:
     * <ul>
     * <li>additional communication roundtrips with the database, instead of a
     * single batch at transaction commit;
     * <li>the fact that an actual database rollback is needed if the Hibernate
     * transaction rolls back (due to already submitted SQL statements).
     * </ul>
     * <p>In case of an existing Session, FLUSH_EAGER will turn the flush mode
     * to AUTO for the scope of the current operation and issue a flush at the
     * end, resetting the previous flush mode afterwards.
     * @see #setFlushMode
     */
    public static final int FLUSH_EAGER = 2;

    /**
     * Flushing at commit only is intended for units of work where no
     * intermediate flushing is desired, not even for find operations
     * that might involve already modified instances.
     * <p>In case of an existing Session, FLUSH_COMMIT will turn the flush mode
     * to COMMIT for the scope of the current operation, resetting the previous
     * flush mode afterwards. The only exception is an existing flush mode
     * NEVER, which will not be modified through this setting.
     * @see #setFlushMode
     */
    public static final int FLUSH_COMMIT = 3;

    /**
     * Flushing before every query statement is rarely necessary.
     * It is only available for special needs.
     * <p>In case of an existing Session, FLUSH_ALWAYS will turn the flush mode
     * to ALWAYS for the scope of the current operation, resetting the previous
     * flush mode afterwards.
     * @see #setFlushMode
     */
    public static final int FLUSH_ALWAYS = 4;

    /**
     * Set the flush behavior to one of the constants in this class. Default is
     * FLUSH_AUTO.
     *
     * @see #setFlushModeName
     * @see #FLUSH_AUTO
     */
    public void setFlushMode(int flushMode) {
        this.flushMode = flushMode;
    }

    /**
     * Return if a flush should be forced after executing the callback code.
     */
    public int getFlushMode() {
        return flushMode;
    }

    /**
     * Apply the flush mode that's been specified for this accessor to the given Session.
     *
     * @param session the current Hibernate Session
     * @param existingTransaction if executing within an existing transaction
     * @return the previous flush mode to restore after the operation, or <code>null</code> if none
     * @see #setFlushMode
     * @see org.hibernate.Session#setFlushMode
     */
    protected FlushMode applyFlushMode(Session session, boolean existingTransaction) {
        if (getFlushMode() == FLUSH_NEVER) {
            if (existingTransaction) {
                FlushMode previousFlushMode = session.getFlushMode();
                if (!previousFlushMode.lessThan(FlushMode.COMMIT)) {
                    session.setFlushMode(FlushMode.MANUAL);
                    return previousFlushMode;
                }
            }
            else {
                session.setFlushMode(FlushMode.MANUAL);
            }
        }
        else if (getFlushMode() == FLUSH_EAGER) {
            if (existingTransaction) {
                FlushMode previousFlushMode = session.getFlushMode();
                if (!previousFlushMode.equals(FlushMode.AUTO)) {
                    session.setFlushMode(FlushMode.AUTO);
                    return previousFlushMode;
                }
            }
            else {
                // rely on default FlushMode.AUTO
            }
        }
        else if (getFlushMode() == FLUSH_COMMIT) {
            if (existingTransaction) {
                FlushMode previousFlushMode = session.getFlushMode();
                if (previousFlushMode.equals(FlushMode.AUTO) || previousFlushMode.equals(FlushMode.ALWAYS)) {
                    session.setFlushMode(FlushMode.COMMIT);
                    return previousFlushMode;
                }
            }
            else {
                session.setFlushMode(FlushMode.COMMIT);
            }
        }
        else if (getFlushMode() == FLUSH_ALWAYS) {
            if (existingTransaction) {
                FlushMode previousFlushMode = session.getFlushMode();
                if (!previousFlushMode.equals(FlushMode.ALWAYS)) {
                    session.setFlushMode(FlushMode.ALWAYS);
                    return previousFlushMode;
                }
            }
            else {
                session.setFlushMode(FlushMode.ALWAYS);
            }
        }
        return null;
    }

    protected void flushIfNecessary(Session session, boolean existingTransaction) throws HibernateException {
        if (getFlushMode() == FLUSH_EAGER || (!existingTransaction && getFlushMode() != FLUSH_NEVER)) {
            logger.debug("Eagerly flushing Hibernate session");
            session.flush();
        }
    }

    protected DataAccessException convertHibernateAccessException(HibernateException ex) {
        if (ex instanceof JDBCException) {
            return convertJdbcAccessException((JDBCException) ex, jdbcExceptionTranslator);
        }
        if (GenericJDBCException.class.equals(ex.getClass())) {
            return convertJdbcAccessException((GenericJDBCException) ex, jdbcExceptionTranslator);
        }
        return SessionFactoryUtils.convertHibernateAccessException(ex);
    }

    protected DataAccessException convertJdbcAccessException(JDBCException ex, SQLExceptionTranslator translator) {
        return translator.translate("Hibernate operation: " + ex.getMessage(), ex.getSQL(), ex.getSQLException());
    }
}
