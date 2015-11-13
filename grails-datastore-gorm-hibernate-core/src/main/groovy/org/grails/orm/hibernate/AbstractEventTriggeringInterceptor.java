/*
 * Copyright 2011 SpringSource.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.orm.hibernate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.grails.datastore.mapping.core.Datastore;
import org.grails.datastore.mapping.engine.event.AbstractPersistenceEvent;
import org.grails.datastore.mapping.engine.event.AbstractPersistenceEventListener;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.orm.hibernate.cfg.AbstractGrailsDomainBinder;
import org.grails.orm.hibernate.cfg.Mapping;
import org.grails.orm.hibernate.support.SoftKey;
import org.hibernate.SessionFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * <p>Invokes closure events on domain entities such as beforeInsert, beforeUpdate and beforeDelete.
 *
 * @author Graeme Rocher
 * @author Lari Hotari
 * @author Burt Beckwith
 * @since 2.0
 */
public abstract class AbstractEventTriggeringInterceptor extends AbstractPersistenceEventListener {

    protected transient ConcurrentMap<SoftKey<Class<?>>, Boolean> cachedShouldTrigger =
            new ConcurrentHashMap<SoftKey<Class<?>>, Boolean>();
    protected boolean failOnError;
    protected List<?> failOnErrorPackages = Collections.emptyList();
    protected Log log = LogFactory.getLog(getClass());

    protected AbstractEventTriggeringInterceptor(Datastore datastore) {
        super(datastore);
    }

    protected boolean isDefinedByCurrentDataStore(Object entity, AbstractGrailsDomainBinder binder) {
        SessionFactory currentDataStoreSessionFactory = ((AbstractHibernateDatastore) datastore).getSessionFactory();
        ApplicationContext applicationContext = datastore.getApplicationContext();
        final MappingContext hibernateMappingContext = datastore.getMappingContext();

        Mapping mapping = binder.getMapping(entity.getClass());
        List<String> dataSourceNames = null;
        if (mapping == null) {
            final PersistentEntity dc = hibernateMappingContext.getPersistentEntity(entity.getClass().getName());
            if (dc != null) {
                dataSourceNames = getDatasourceNames(dc);
            }
        }
        else {
            dataSourceNames = mapping.getDatasources();
        }

        if (dataSourceNames == null) {
            return false;
        }

        for (String dataSource : dataSourceNames) {
            if (Mapping.ALL_DATA_SOURCES.equals(dataSource)) {
                return true;
            }
            boolean isDefault = dataSource.equals(Mapping.DEFAULT_DATA_SOURCE);
            String suffix = isDefault ? "" : "_" + dataSource;
            String sessionFactoryBeanName = "sessionFactory" + suffix;

            if (applicationContext.containsBean(sessionFactoryBeanName)) {
                SessionFactory sessionFactory = applicationContext.getBean(sessionFactoryBeanName, SessionFactory.class);
                if (currentDataStoreSessionFactory == sessionFactory) {
                    return true;
                }
            }
            else {
                log.warn("Cannot resolve SessionFactory for dataSource ["+dataSource+"] and entity ["+entity.getClass().getName()+"]");
            }
        }
        return false;
    }

    protected abstract List<String> getDatasourceNames(PersistentEntity dc);

    /**
     * {@inheritDoc}
     * @see org.springframework.context.event.SmartApplicationListener#supportsEventType(
     *     java.lang.Class)
     */
    public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        return AbstractPersistenceEvent.class.isAssignableFrom(eventType);
    }
}
