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
package org.grails.orm.hibernate;

import java.util.Map;
import java.util.concurrent.Callable;

import org.grails.datastore.mapping.core.Session;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.orm.hibernate.cfg.Mapping;
import org.hibernate.FlushMode;
import org.hibernate.SessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.PropertyResolver;

/**
 * Datastore implementation that uses a Hibernate SessionFactory underneath.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class HibernateDatastore extends AbstractHibernateDatastore  {

    public HibernateDatastore(MappingContext mappingContext, SessionFactory sessionFactory, PropertyResolver config) {
        super(mappingContext, sessionFactory, config, null, Mapping.DEFAULT_DATA_SOURCE);
    }

    public HibernateDatastore(MappingContext mappingContext, SessionFactory sessionFactory, PropertyResolver config, ApplicationContext applicationContext) {
        super(mappingContext, sessionFactory, config, applicationContext, Mapping.DEFAULT_DATA_SOURCE);
    }

    public HibernateDatastore(MappingContext mappingContext, SessionFactory sessionFactory, PropertyResolver config, String dataSourceName) {
        super(mappingContext, sessionFactory, config, null,dataSourceName);
    }


    @Override
    protected Session createSession(Map<String, String> connectionDetails) {
        return new HibernateSession(this, sessionFactory);
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        super.setApplicationContext(applicationContext);

        if (applicationContext != null) {
            // support for callbacks in domain classes
            eventTriggeringInterceptor = new EventTriggeringInterceptor(this, config);
            ((ConfigurableApplicationContext)applicationContext).addApplicationListener(eventTriggeringInterceptor);
        }
    }

    @Override
    public IHibernateTemplate getHibernateTemplate(int flushMode) {
        return new GrailsHibernateTemplate(getSessionFactory(), this, flushMode);
    }

    @Override
    public void withFlushMode(FlushMode flushMode, Callable<Boolean> callable) {
        final org.hibernate.Session session = sessionFactory.getCurrentSession();
        org.hibernate.FlushMode previousMode = null;
        Boolean reset = true;
        try {
            if (session != null) {
                previousMode = session.getFlushMode();
                session.setFlushMode(org.hibernate.FlushMode.valueOf(flushMode.name()));
            }
            try {
                reset = callable.call();
            } catch (Exception e) {
                reset = false;
            }
        }
        finally {
            if (session != null && previousMode != null && reset) {
                session.setFlushMode(previousMode);
            }
        }
    }

    @Override
    public org.hibernate.Session openSession() {
        return this.sessionFactory.openSession();
    }
}
