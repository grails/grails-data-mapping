/*
 * Copyright 2003-2007 the original author or authors.
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
package org.grails.orm.hibernate.support;


import java.util.*;

import org.grails.datastore.mapping.model.config.GormProperties;
import org.grails.orm.hibernate.AbstractHibernateGormInstanceApi;
import org.grails.orm.hibernate.HibernateDatastore;
import org.grails.orm.hibernate.SessionFactoryProxy;
import org.grails.datastore.mapping.core.Datastore;
import org.grails.datastore.mapping.engine.event.AbstractPersistenceEvent;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.engine.internal.Nullability;
import org.hibernate.event.internal.DefaultSaveOrUpdateEventListener;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.AbstractEvent;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PostLoadEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.event.spi.PreDeleteEvent;
import org.hibernate.event.spi.PreDeleteEventListener;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.PreLoadEvent;
import org.hibernate.event.spi.PreLoadEventListener;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.hibernate.event.spi.SaveOrUpdateEvent;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Listens for Hibernate events and publishes corresponding Datastore events.
 *
 * @author Graeme Rocher
 * @author Lari Hotari
 * @author Burt Beckwith
 * @since 1.0
 */
public class ClosureEventTriggeringInterceptor extends DefaultSaveOrUpdateEventListener
       implements ApplicationContextAware,
                  PreLoadEventListener,
                  PostLoadEventListener,
                  PostInsertEventListener,
                  PostUpdateEventListener,
                  PostDeleteEventListener,
                  PreDeleteEventListener,
                  PreUpdateEventListener,
                  PreInsertEventListener {

//    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final long serialVersionUID = 1;

    public static final Collection<String> IGNORED = new HashSet<String>(Arrays.asList(GormProperties.VERSION, GormProperties.IDENTITY));
    public static final String ONLOAD_EVENT = "onLoad";
    public static final String ONLOAD_SAVE = "onSave";
    public static final String BEFORE_LOAD_EVENT = "beforeLoad";
    public static final String BEFORE_INSERT_EVENT = "beforeInsert";
    public static final String AFTER_INSERT_EVENT = "afterInsert";
    public static final String BEFORE_UPDATE_EVENT = "beforeUpdate";
    public static final String AFTER_UPDATE_EVENT = "afterUpdate";
    public static final String BEFORE_DELETE_EVENT = "beforeDelete";
    public static final String AFTER_DELETE_EVENT = "afterDelete";
    public static final String AFTER_LOAD_EVENT = "afterLoad";

    private ApplicationContext ctx;
    private Map<SessionFactory, HibernateDatastore> datastores;


    public void setDatastores(HibernateDatastore[] datastores) {
        Map<SessionFactory, HibernateDatastore> datastoreMap = new HashMap<SessionFactory, HibernateDatastore>();
        for (HibernateDatastore hibernateDatastore : datastores) {
            datastoreMap.put(hibernateDatastore.getSessionFactory(), hibernateDatastore);
        }
        this.datastores = datastoreMap;
    }

    @Override
    public void onSaveOrUpdate(SaveOrUpdateEvent hibernateEvent) throws HibernateException {
        publishEvent(hibernateEvent, new org.grails.datastore.mapping.engine.event.SaveOrUpdateEvent(
                findDatastore(hibernateEvent), hibernateEvent.getEntity()));
        super.onSaveOrUpdate(hibernateEvent);
    }

    public void onPreLoad(PreLoadEvent hibernateEvent) {
        publishEvent(hibernateEvent, new org.grails.datastore.mapping.engine.event.PreLoadEvent(
                findDatastore(hibernateEvent), hibernateEvent.getEntity()));
    }

    public void onPostLoad(PostLoadEvent hibernateEvent) {
        publishEvent(hibernateEvent, new org.grails.datastore.mapping.engine.event.PostLoadEvent(
                findDatastore(hibernateEvent), hibernateEvent.getEntity()));
    }

    public boolean onPreInsert(PreInsertEvent hibernateEvent) {
        AbstractPersistenceEvent event = new org.grails.datastore.mapping.engine.event.PreInsertEvent(
                findDatastore(hibernateEvent), hibernateEvent.getEntity());
        publishEvent(hibernateEvent, event);
        return event.isCancelled();
    }

    public void onPostInsert(PostInsertEvent hibernateEvent) {
        publishEvent(hibernateEvent, new org.grails.datastore.mapping.engine.event.PostInsertEvent(
                findDatastore(hibernateEvent), hibernateEvent.getEntity()));
    }

    @Override
    public boolean requiresPostCommitHanding(EntityPersister persister) {
        return false;
    }

    public boolean onPreUpdate(PreUpdateEvent hibernateEvent) {
        AbstractPersistenceEvent event = new org.grails.datastore.mapping.engine.event.PreUpdateEvent(
                findDatastore(hibernateEvent), hibernateEvent.getEntity());
        publishEvent(hibernateEvent, event);
        return event.isCancelled();
    }

    public void onPostUpdate(PostUpdateEvent hibernateEvent) {
        publishEvent(hibernateEvent, new org.grails.datastore.mapping.engine.event.PostUpdateEvent(
                findDatastore(hibernateEvent), hibernateEvent.getEntity()));
    }

    public boolean onPreDelete(PreDeleteEvent hibernateEvent) {
        AbstractPersistenceEvent event = new org.grails.datastore.mapping.engine.event.PreDeleteEvent(
                findDatastore(hibernateEvent), hibernateEvent.getEntity());
        publishEvent(hibernateEvent, event);
        return event.isCancelled();
    }

    public void onPostDelete(PostDeleteEvent hibernateEvent) {
        publishEvent(hibernateEvent, new org.grails.datastore.mapping.engine.event.PostDeleteEvent(
                findDatastore(hibernateEvent), hibernateEvent.getEntity()));
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        ctx = applicationContext;
    }

    private void publishEvent(AbstractEvent hibernateEvent, AbstractPersistenceEvent mappingEvent) {
        mappingEvent.setNativeEvent(hibernateEvent);
        ctx.publishEvent(mappingEvent);
    }

    private Datastore findDatastore(AbstractEvent hibernateEvent) {
        SessionFactory sessionFactory = hibernateEvent.getSession().getSessionFactory();
        if (!(sessionFactory instanceof SessionFactoryProxy)) {
            // should always be the case
            for (Map.Entry<SessionFactory, HibernateDatastore> entry : datastores.entrySet()) {
                SessionFactory sf = entry.getKey();
                if (sf instanceof SessionFactoryProxy) {
                    if (((SessionFactoryProxy)sf).getCurrentSessionFactory() == sessionFactory) {
                        return entry.getValue();
                    }
                }
            }
        }

        Datastore datastore = datastores.get(sessionFactory);
        if (datastore == null && datastores.size() == 1) {
            datastore = datastores.values().iterator().next();
        }
        return datastore;
    }

    /*
     * TODO: This is a horrible hack due to a bug in Hibernate's post-insert event processing (HHH-3904)
     */
/*    @Override
    protected Serializable performSaveOrReplicate(Object entity, EntityKey key, EntityPersister persister, boolean useIdentityColumn,
            Object anything, EventSource source, boolean requiresImmediateIdAccess) {
*/

    public static final void addNullabilityCheckerPreInsertEventListener(EventListenerRegistry listenerRegistry) {
        listenerRegistry.getEventListenerGroup(EventType.PRE_INSERT).appendListener(NULLABILITY_CHECKER_INSTANCE);
    }

    private static final PreInsertEventListener NULLABILITY_CHECKER_INSTANCE = new NullabilityCheckerPreInsertEventListener();

    @SuppressWarnings("serial")
    private static class NullabilityCheckerPreInsertEventListener implements PreInsertEventListener {
        public boolean onPreInsert(PreInsertEvent event) {
            new Nullability(event.getSession()).checkNullability(event.getState(), event.getPersister(), false);
            return false;
        }
    }

    /**
     * Prevents hitting the database for an extra check if the row exists in the database.
     *
     * ThreadLocal is used to pass the "insert:true" information to Hibernate.
     *
     */
    @Override
    protected Boolean getAssumedUnsaved() {
        return AbstractHibernateGormInstanceApi.getAssumedUnsaved();
    }


}
