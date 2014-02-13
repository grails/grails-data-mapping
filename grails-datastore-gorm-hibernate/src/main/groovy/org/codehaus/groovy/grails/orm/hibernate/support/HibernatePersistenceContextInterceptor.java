/*
 * Copyright 2004-2006 Graeme Rocher
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
package org.codehaus.groovy.grails.orm.hibernate.support;

import grails.validation.DeferredBindingActions;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.lifecycle.ShutdownOperations;
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil;
import org.codehaus.groovy.grails.orm.hibernate.metaclass.AbstractSavePersistentMethod;
import org.codehaus.groovy.grails.support.PersistenceContextInterceptor;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.orm.hibernate3.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Graeme Rocher
 * @since 0.4
 */
public class HibernatePersistenceContextInterceptor implements PersistenceContextInterceptor, SessionFactoryAwarePersistenceContextInterceptor {

    private static final Log LOG = LogFactory.getLog(HibernatePersistenceContextInterceptor.class);

    private static ThreadLocal<Map<String, Boolean>> participate = new ThreadLocal<Map<String, Boolean>>() {
        @Override
        protected Map<String, Boolean> initialValue() {
            return new HashMap<String, Boolean>();
        }
    };

    private static ThreadLocal<Map<String, Integer>> nestingCount = new ThreadLocal<Map<String, Integer>>() {
        @Override
        protected Map<String, Integer> initialValue() {
            return new HashMap<String, Integer>();
        }
    };

    static {
        ShutdownOperations.addOperation(new Runnable() {
            public void run() {
                participate.remove();
                nestingCount.remove();
            }
        });
    }

    private SessionFactory sessionFactory;
    private String dataSourceName;

    public HibernatePersistenceContextInterceptor() {
        this.dataSourceName = GrailsDomainClassProperty.DEFAULT_DATA_SOURCE;
    }

    /**
     * @param dataSourceName a name of dataSource
     */
    public HibernatePersistenceContextInterceptor(String dataSourceName) {
        this.dataSourceName = dataSourceName;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.support.PersistenceContextInterceptor#destroy()
     */
    public void destroy() {
        DeferredBindingActions.clear();
        if (decNestingCount() > 0 || getParticipate()) {
            return;
        }

        try {
            // single session mode
            SessionHolder holder = (SessionHolder) TransactionSynchronizationManager.unbindResource(getSessionFactory());
            LOG.debug("Closing single Hibernate session in GrailsDispatcherServlet");
            try {
                Session session = holder.getSession();
                SessionFactoryUtils.closeSession(session);
            } catch (RuntimeException ex) {
                LOG.error("Unexpected exception on closing Hibernate Session", ex);
            }
        } finally {
            AbstractSavePersistentMethod.clearDisabledValidations();
        }
    }

    public void disconnect() {
        try {
            getSession(false).disconnect();
        } catch (IllegalStateException e) {
            // no session ignore
        }
    }

    @SuppressWarnings("deprecation")
    public void reconnect() {
        getSession().reconnect();
    }

    public void flush() {
        getSession().flush();
    }

    public void clear() {
        getSession().clear();
    }

    public void setReadOnly() {
        getSession().setFlushMode(FlushMode.MANUAL);
    }

    public void setReadWrite() {
        getSession().setFlushMode(FlushMode.AUTO);
    }

    public boolean isOpen() {
        try {
            return getSession(false).isOpen();
        } catch (Exception e) {
            return false;
        }
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.support.PersistenceContextInterceptor#init()
     */
    public void init() {
        if (incNestingCount() > 1) {
            return;
        }
        SessionFactory sf = getSessionFactory();
        if (TransactionSynchronizationManager.hasResource(sf)) {
            // Do not modify the Session: just set the participate flag.
            setParticipate(true);
        } else {
            setParticipate(false);
            LOG.debug("Opening single Hibernate session in HibernatePersistenceContextInterceptor");
            Session session = getSession();
            GrailsHibernateUtil.enableDynamicFilterEnablerIfPresent(sf, session);
            session.setFlushMode(FlushMode.AUTO);
            TransactionSynchronizationManager.bindResource(sf, new SessionHolder(session));
        }
    }

    private Session getSession() {
        return getSession(true);
    }

    private Session getSession(boolean allowCreate) {
        return SessionFactoryUtils.getSession(getSessionFactory(), allowCreate);
    }

    /**
     * @return the sessionFactory
     */
    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    /**
     * @param sessionFactory the sessionFactory to set
     */
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    private int incNestingCount() {
        Map<String, Integer> map = nestingCount.get();
        Integer current = map.get(dataSourceName);
        int value = (current != null) ? current + 1 : 1;
        map.put(dataSourceName, value);
        return value;
    }

    private int decNestingCount() {
        Map<String, Integer> map = nestingCount.get();
        Integer current = map.get(dataSourceName);
        int value = (current != null) ? current - 1 : 0;
        if (value < 0) {
            value = 0;
        }
        map.put(dataSourceName, value);
        return value;
    }

    private void setParticipate(boolean flag) {
        Map<String, Boolean> map = participate.get();
        map.put(dataSourceName, flag);
    }

    private boolean getParticipate() {
        Map<String, Boolean> map = participate.get();
        Boolean ret = map.get(dataSourceName);
        return (ret != null) ? ret : false;
    }
}
