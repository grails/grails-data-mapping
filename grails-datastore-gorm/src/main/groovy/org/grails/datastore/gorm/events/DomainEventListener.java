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
package org.grails.datastore.gorm.events;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.datastore.mapping.core.Datastore;
import org.springframework.datastore.mapping.engine.EntityAccess;
import org.springframework.datastore.mapping.engine.event.AbstractPersistenceEvent;
import org.springframework.datastore.mapping.engine.event.AbstractPersistenceEventListener;
import org.springframework.datastore.mapping.engine.event.PostDeleteEvent;
import org.springframework.datastore.mapping.engine.event.PostInsertEvent;
import org.springframework.datastore.mapping.engine.event.PostLoadEvent;
import org.springframework.datastore.mapping.engine.event.PostUpdateEvent;
import org.springframework.datastore.mapping.engine.event.PreDeleteEvent;
import org.springframework.datastore.mapping.engine.event.PreInsertEvent;
import org.springframework.datastore.mapping.engine.event.PreLoadEvent;
import org.springframework.datastore.mapping.engine.event.PreUpdateEvent;
import org.springframework.datastore.mapping.model.MappingContext;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.util.ReflectionUtils;

/**
 * An event listener that provides support for GORM domain events.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class DomainEventListener extends AbstractPersistenceEventListener
       implements MappingContext.Listener {

    private Map<PersistentEntity, Map<String, Method>> entityEvents = new ConcurrentHashMap<PersistentEntity, Map<String, Method>>();

    public static final Class[] ZERO_PARAMS = {};
    public static final String EVENT_BEFORE_INSERT  = "beforeInsert";
    private static final String EVENT_BEFORE_UPDATE = "beforeUpdate";
    private static final String EVENT_BEFORE_DELETE = "beforeDelete";
    private static final String EVENT_BEFORE_LOAD   = "beforeLoad";
    private static final String EVENT_AFTER_INSERT  = "afterInsert";
    private static final String EVENT_AFTER_UPDATE  = "afterUpdate";
    private static final String EVENT_AFTER_DELETE  = "afterDelete";
    private static final String EVENT_AFTER_LOAD    = "afterLoad";

    private static final List<String> REFRESH_EVENTS = Arrays.asList(
            EVENT_BEFORE_INSERT, EVENT_BEFORE_UPDATE, EVENT_BEFORE_DELETE);

    public DomainEventListener(final Datastore datastore) {
        super(datastore);

        for (PersistentEntity entity : datastore.getMappingContext().getPersistentEntities()) {
            createEventCaches(entity);
        }

        datastore.getMappingContext().addMappingContextListener(this);
    }

    @Override
    protected void onPersistenceEvent(final AbstractPersistenceEvent event) {
        if (event instanceof PreInsertEvent) {
            beforeInsert(event.getEntity(), event.getEntityAccess());
        }
        else if (event instanceof PostInsertEvent) {
            afterInsert(event.getEntity(), event.getEntityAccess());
        }
        else if (event instanceof PreUpdateEvent) {
            beforeUpdate(event.getEntity(), event.getEntityAccess());
        }
        else if (event instanceof PostUpdateEvent) {
            afterUpdate(event.getEntity(), event.getEntityAccess());
        }
        else if (event instanceof PreDeleteEvent) {
            beforeDelete(event.getEntity(), event.getEntityAccess());
        }
        else if (event instanceof PostDeleteEvent) {
            afterDelete(event.getEntity(), event.getEntityAccess());
        }
        else if (event instanceof PreLoadEvent) {
            beforeLoad(event.getEntity(), event.getEntityAccess());
        }
        else if (event instanceof PostLoadEvent) {
            afterLoad(event.getEntity(), event.getEntityAccess());
        }
    }

    public boolean beforeInsert(final PersistentEntity entity, final EntityAccess ea) {
        return invokeEvent(EVENT_BEFORE_INSERT, entity, ea);
    }

    public boolean beforeUpdate(final PersistentEntity entity, final EntityAccess ea) {
        return invokeEvent(EVENT_BEFORE_UPDATE, entity, ea);
    }

    public boolean beforeDelete(final PersistentEntity entity, final EntityAccess ea) {
        return invokeEvent(EVENT_BEFORE_DELETE, entity, ea);
    }

    public void beforeLoad(final PersistentEntity entity, final EntityAccess ea) {
        invokeEvent(EVENT_BEFORE_LOAD, entity, ea);
    }

    public void afterDelete(final PersistentEntity entity, final EntityAccess ea) {
        invokeEvent(EVENT_AFTER_DELETE, entity, ea);
    }

    public void afterInsert(final PersistentEntity entity, final EntityAccess ea) {
        invokeEvent(EVENT_AFTER_INSERT, entity, ea);
    }

    public void afterUpdate(final PersistentEntity entity, final EntityAccess ea) {
        invokeEvent(EVENT_AFTER_UPDATE, entity, ea);
    }

    public void afterLoad(final PersistentEntity entity, final EntityAccess ea) {
        autowireBeanProperties(ea.getEntity());
        invokeEvent(EVENT_AFTER_LOAD, entity, ea);
    }

    public void autowireBeanProperties(final Object entity) {
        datastore.getApplicationContext().getAutowireCapableBeanFactory().autowireBeanProperties(
              entity, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
    }

    /**
     * {@inheritDoc}
     * @see org.springframework.datastore.mapping.model.MappingContext.Listener#persistentEntityAdded(
     *     org.springframework.datastore.mapping.model.PersistentEntity)
     */
    public void persistentEntityAdded(PersistentEntity entity) {
        createEventCaches(entity);
    }

    /**
     * {@inheritDoc}
     * @see org.springframework.context.event.SmartApplicationListener#supportsEventType(
     *     java.lang.Class)
     */
    public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        return AbstractPersistenceEvent.class.isAssignableFrom(eventType);
    }

    private boolean invokeEvent(String eventName, PersistentEntity entity, EntityAccess ea) {
        final Map<String, Method> events = entityEvents.get(entity);
        if (events == null) {
            return true;
        }

        final Method eventMethod = events.get(eventName);
        if (eventMethod == null) {
            return true;
        }

        final Object result = ReflectionUtils.invokeMethod(eventMethod, ea.getEntity());
        boolean booleanResult = (result instanceof Boolean) ? (Boolean)result : true;
        if (booleanResult && REFRESH_EVENTS.contains(eventName)) {
            ea.refresh();
        }
        return booleanResult;
    }

    private void createEventCaches(PersistentEntity entity) {
        Class javaClass = entity.getJavaClass();
        final ConcurrentHashMap<String, Method> events = new ConcurrentHashMap<String, Method>();
        entityEvents.put(entity, events);

        findAndCacheEvent(EVENT_BEFORE_INSERT, javaClass, events);
        findAndCacheEvent(EVENT_BEFORE_UPDATE, javaClass, events);
        findAndCacheEvent(EVENT_BEFORE_DELETE, javaClass, events);
        findAndCacheEvent(EVENT_BEFORE_LOAD,   javaClass, events);
        findAndCacheEvent(EVENT_AFTER_INSERT,  javaClass, events);
        findAndCacheEvent(EVENT_AFTER_UPDATE,  javaClass, events);
        findAndCacheEvent(EVENT_AFTER_DELETE,  javaClass, events);
        findAndCacheEvent(EVENT_AFTER_LOAD,    javaClass, events);
    }

    private void findAndCacheEvent(String event, Class javaClass, Map<String, Method> events) {
        final Method method = ReflectionUtils.findMethod(javaClass, event);
        if (method != null) {
            events.put(event, method);
        }
    }
}
