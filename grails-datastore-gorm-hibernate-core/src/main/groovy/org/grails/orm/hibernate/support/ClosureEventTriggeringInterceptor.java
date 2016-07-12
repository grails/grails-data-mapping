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


import org.grails.datastore.gorm.events.ConfigurableApplicationContextEventPublisher;
import org.grails.datastore.gorm.events.ConfigurableApplicationEventPublisher;
import org.grails.datastore.mapping.engine.event.AbstractPersistenceEvent;
import org.grails.datastore.mapping.model.config.GormProperties;
import org.grails.orm.hibernate.AbstractHibernateDatastore;
import org.grails.orm.hibernate.AbstractHibernateGormInstanceApi;
import org.hibernate.HibernateException;
import org.hibernate.engine.internal.Nullability;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.*;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

/**
 * Listens for Hibernate events and publishes corresponding Datastore events.
 *
 * @author Graeme Rocher
 * @author Lari Hotari
 * @author Burt Beckwith
 * @since 1.0
 */
public class ClosureEventTriggeringInterceptor extends AbstractClosureEventTriggeringInterceptor {

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

    protected AbstractHibernateDatastore datastore;
    protected ConfigurableApplicationEventPublisher eventPublisher;

    public void setDatastore(AbstractHibernateDatastore datastore) {
        this.datastore = datastore;
    }

    public void setEventPublisher(ConfigurableApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void onSaveOrUpdate(SaveOrUpdateEvent hibernateEvent) throws HibernateException {
        Object entity = getEntity(hibernateEvent);
        if(entity != null) {
            publishEvent(hibernateEvent, new org.grails.datastore.mapping.engine.event.SaveOrUpdateEvent(
                    this.datastore, entity));
        }
        super.onSaveOrUpdate(hibernateEvent);
    }

    protected Object getEntity(SaveOrUpdateEvent hibernateEvent) {
        Object object = hibernateEvent.getObject();
        if(object != null) {
            return object;
        }
        else {
            return hibernateEvent.getEntity();
        }
    }

    public void onPreLoad(PreLoadEvent hibernateEvent) {
        publishEvent(hibernateEvent, new org.grails.datastore.mapping.engine.event.PreLoadEvent(
                this.datastore, hibernateEvent.getEntity()));
    }

    public void onPostLoad(PostLoadEvent hibernateEvent) {
        publishEvent(hibernateEvent, new org.grails.datastore.mapping.engine.event.PostLoadEvent(
                this.datastore, hibernateEvent.getEntity()));
    }

    public boolean onPreInsert(PreInsertEvent hibernateEvent) {
        AbstractPersistenceEvent event = new org.grails.datastore.mapping.engine.event.PreInsertEvent(
                this.datastore, hibernateEvent.getEntity());
        publishEvent(hibernateEvent, event);
        return event.isCancelled();
    }

    public void onPostInsert(PostInsertEvent hibernateEvent) {
        publishEvent(hibernateEvent, new org.grails.datastore.mapping.engine.event.PostInsertEvent(
                this.datastore, hibernateEvent.getEntity()));
    }

    @Override
    public boolean requiresPostCommitHanding(EntityPersister persister) {
        return false;
    }

    public boolean onPreUpdate(PreUpdateEvent hibernateEvent) {
        AbstractPersistenceEvent event = new org.grails.datastore.mapping.engine.event.PreUpdateEvent(
                this.datastore, hibernateEvent.getEntity());
        publishEvent(hibernateEvent, event);
        return event.isCancelled();
    }

    public void onPostUpdate(PostUpdateEvent hibernateEvent) {
        publishEvent(hibernateEvent, new org.grails.datastore.mapping.engine.event.PostUpdateEvent(
                this.datastore, hibernateEvent.getEntity()));
    }

    public boolean onPreDelete(PreDeleteEvent hibernateEvent) {
        AbstractPersistenceEvent event = new org.grails.datastore.mapping.engine.event.PreDeleteEvent(
                this.datastore, hibernateEvent.getEntity());
        publishEvent(hibernateEvent, event);
        return event.isCancelled();
    }

    public void onPostDelete(PostDeleteEvent hibernateEvent) {
        publishEvent(hibernateEvent, new org.grails.datastore.mapping.engine.event.PostDeleteEvent(
                this.datastore, hibernateEvent.getEntity()));
    }

    private void publishEvent(AbstractEvent hibernateEvent, AbstractPersistenceEvent mappingEvent) {
        mappingEvent.setNativeEvent(hibernateEvent);
        if(eventPublisher != null) {
            eventPublisher.publishEvent(mappingEvent);
        }
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

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if(applicationContext instanceof ConfigurableApplicationContext) {

            this.eventPublisher = new ConfigurableApplicationContextEventPublisher((ConfigurableApplicationContext) applicationContext);
        }
    }

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
