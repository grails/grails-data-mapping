/*
 * Copyright 2004-2005 Graeme Rocher
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
package org.grails.orm.hibernate4.support;

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.grails.orm.hibernate.AbstractHibernateDatastore;
import org.grails.orm.hibernate.support.HibernateRuntimeUtils;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.orm.hibernate4.SessionHolder;
import org.springframework.orm.hibernate4.support.OpenSessionInViewInterceptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.WebRequest;


/**
 * Extends the default spring OSIVI and doesn't flush the session if it has been set
 * to MANUAL on the session itself.
 *
 * @author Graeme Rocher
 * @since 0.5
 */
public class GrailsOpenSessionInViewInterceptor extends OpenSessionInViewInterceptor {
    protected static final String IS_FLOW_REQUEST_ATTRIBUTE = "org.codehaus.groovy.grails.webflow.flow_request";

    protected AbstractHibernateDatastore.FlushMode flushMode = AbstractHibernateDatastore.FlushMode.AUTO;

    @Override
    public void preHandle(WebRequest request) throws DataAccessException {
        GrailsWebRequest webRequest = GrailsWebRequest.lookup();
        final boolean isFlowRequest = webRequest != null && webRequest.isFlowRequest();
        if (isFlowRequest) {
            webRequest.setAttribute(IS_FLOW_REQUEST_ATTRIBUTE, "true", WebRequest.SCOPE_REQUEST);
        }
        else {
            super.preHandle(request);
            SessionFactory sessionFactory = getSessionFactory();
            SessionHolder sessionHolder = (SessionHolder)TransactionSynchronizationManager.getResource(sessionFactory);
            if (sessionHolder != null) {
                Session session = sessionHolder.getSession();
                HibernateRuntimeUtils.enableDynamicFilterEnablerIfPresent(sessionFactory, session);
            }
        }
    }

    @Override
    protected Session openSession() throws DataAccessResourceFailureException {
        Session session = super.openSession();
        applyFlushMode(session);
        return session;
    }

    protected void applyFlushMode(Session session) {
        FlushMode hibernateFlushMode = FlushMode.AUTO;
        switch (flushMode) {
            case AUTO:
                hibernateFlushMode = FlushMode.AUTO;
                break;
            case MANUAL:
                hibernateFlushMode = FlushMode.MANUAL;
                break;
            case COMMIT:
                hibernateFlushMode = FlushMode.COMMIT;
                break;
            case ALWAYS:
                hibernateFlushMode = FlushMode.ALWAYS;
                break;
        }
        session.setFlushMode(hibernateFlushMode);
    }

    @Override
    public void postHandle(WebRequest request, ModelMap model) throws DataAccessException {
        final boolean isFlowRequest = request.getAttribute(IS_FLOW_REQUEST_ATTRIBUTE, WebRequest.SCOPE_REQUEST) != null;
        if (isFlowRequest) {
            return;
        }

        SessionHolder sessionHolder = (SessionHolder)TransactionSynchronizationManager.getResource(getSessionFactory());
        Session session = sessionHolder != null ? sessionHolder.getSession() : null;
        try {
            super.postHandle(request, model);
            if (session != null && flushMode != AbstractHibernateDatastore.FlushMode.MANUAL && !FlushMode.isManualFlushMode(session.getFlushMode())) {
                logger.debug("Eagerly flushing Hibernate session");
                session.flush();
            }
        }
        finally {
            if (session != null) {
                session.setFlushMode(FlushMode.MANUAL);
            }
        }
    }

    @Override
    public void afterCompletion(WebRequest request, Exception ex) throws DataAccessException {
        final boolean isWebRequest = request.getAttribute(IS_FLOW_REQUEST_ATTRIBUTE, WebRequest.SCOPE_REQUEST) != null;
        if (isWebRequest) {
            return;
        }

        super.afterCompletion(request, ex);
    }


    public void setFlushMode(int flushMode) {
        if(AbstractHibernateDatastore.FlushMode.AUTO.getLevel() == flushMode) {
            this.flushMode = AbstractHibernateDatastore.FlushMode.AUTO;
        }
        else if(AbstractHibernateDatastore.FlushMode.MANUAL.getLevel() == flushMode) {
            this.flushMode = AbstractHibernateDatastore.FlushMode.MANUAL;
        }
        else if(AbstractHibernateDatastore.FlushMode.COMMIT.getLevel() == flushMode) {
            this.flushMode = AbstractHibernateDatastore.FlushMode.COMMIT;
        }
        else if(AbstractHibernateDatastore.FlushMode.ALWAYS.getLevel() == flushMode) {
            this.flushMode = AbstractHibernateDatastore.FlushMode.ALWAYS;
        }
    }
    public int getFlushMode() {
        return flushMode.getLevel();
    }
}
