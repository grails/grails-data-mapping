/*
 * Copyright 2011 SpringSource.
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

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class GrailsHibernateTemplate extends HibernateTemplate implements IHibernateTemplate {
    private boolean osivReadOnly;
    private boolean passReadOnlyToHibernate = false;

    public GrailsHibernateTemplate() {
        initialize(null);
    }

    public GrailsHibernateTemplate(SessionFactory sessionFactory, boolean allowCreate) {
        super(sessionFactory, allowCreate);
        initialize(null);
    }

    public GrailsHibernateTemplate(SessionFactory sessionFactory) {
        super(sessionFactory);
        initialize(null);
    }

    public GrailsHibernateTemplate(SessionFactory sessionFactory, GrailsApplication application) {
        super(sessionFactory);
        initialize(application);
    }

    private void initialize(GrailsApplication application) {
        setExposeNativeSession(true);
        if (application != null) {
            setCacheQueries(GrailsHibernateUtil.isCacheQueriesByDefault(application));
            this.osivReadOnly = GrailsHibernateUtil.isOsivReadonly(application);
            this.passReadOnlyToHibernate = GrailsHibernateUtil.isPassReadOnlyToHibernate(application);
        }
    }

    @Override
    protected void prepareQuery(Query queryObject) {
        super.prepareQuery(queryObject);
        if(shouldPassReadOnlyToHibernate()) {
            queryObject.setReadOnly(true);
        }
    }

    public void applySettings(Query queryObject) {
        if (isExposeNativeSession()) {
            prepareQuery(queryObject);
        }
    }

    @Override
    protected void prepareCriteria(Criteria criteria) {
        super.prepareCriteria(criteria);
        if(shouldPassReadOnlyToHibernate()) {
            criteria.setReadOnly(true);
        }
    }

    protected boolean shouldPassReadOnlyToHibernate() {
        if((passReadOnlyToHibernate || osivReadOnly) && TransactionSynchronizationManager.hasResource(getSessionFactory())) {
            if(TransactionSynchronizationManager.isActualTransactionActive()) {
                return passReadOnlyToHibernate && TransactionSynchronizationManager.isCurrentTransactionReadOnly();
            } else {
                return osivReadOnly;
            }
        } else {
            return false;
        }
    }

    public void applySettings(Criteria criteria) {
        if (isExposeNativeSession()) {
            prepareCriteria(criteria);
        }
    }

    @Override
    protected void enableFilters(Session session) {
        if(shouldPassReadOnlyToHibernate()) {
            session.setDefaultReadOnly(true);
        }
        super.enableFilters(session);
    }

    public boolean isOsivReadOnly() {
        return osivReadOnly;
    }

    public void setOsivReadOnly(boolean osivReadOnly) {
        this.osivReadOnly = osivReadOnly;
    }
}
