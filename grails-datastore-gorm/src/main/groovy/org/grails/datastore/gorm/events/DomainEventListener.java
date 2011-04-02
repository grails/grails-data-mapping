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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.ApplicationEvent;
import org.springframework.datastore.mapping.core.Datastore;
import org.springframework.datastore.mapping.engine.EntityAccess;
import org.springframework.datastore.mapping.engine.event.AbstractPersistenceEvent;
import org.springframework.datastore.mapping.engine.event.AbstractPersistenceEventListener;
import org.springframework.datastore.mapping.engine.event.PreDeleteEvent;
import org.springframework.datastore.mapping.engine.event.PreInsertEvent;
import org.springframework.datastore.mapping.engine.event.PreUpdateEvent;
import org.springframework.datastore.mapping.model.MappingContext;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.util.ReflectionUtils;

/**
 * An event listener that provides support for GORM domain events
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class DomainEventListener extends AbstractPersistenceEventListener
       implements MappingContext.Listener {

    private Map<PersistentEntity, Map<String, Method>> entityEvents = new ConcurrentHashMap<PersistentEntity, Map<String, Method>>();

    public static final Class[] ZERO_PARAMS = new Class[0];
    public static final String EVENT_BEFORE_INSERT = "beforeInsert";
    private static final String EVENT_BEFORE_UPDATE = "beforeUpdate";
    private static final String EVENT_BEFORE_DELETE = "beforeDelete";

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
        else if (event instanceof PreUpdateEvent) {
            beforeUpdate(event.getEntity(), event.getEntityAccess());
        }
        else if (event instanceof PreDeleteEvent) {
            beforeDelete(event.getEntity(), event.getEntityAccess());
        }
    }

    public boolean beforeInsert(PersistentEntity entity, EntityAccess ea) {
        return invokeEvent(EVENT_BEFORE_INSERT, entity, ea);
    }

    public boolean beforeUpdate(PersistentEntity entity, EntityAccess ea) {
        return invokeEvent(EVENT_BEFORE_UPDATE, entity, ea);
    }

    public boolean beforeDelete(PersistentEntity entity, EntityAccess ea) {
        return invokeEvent(EVENT_BEFORE_DELETE, entity, ea);
    }

    public void persistentEntityAdded(PersistentEntity entity) {
        createEventCaches(entity);
    }

    public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        return PreDeleteEvent.class.isAssignableFrom(eventType) ||
               PreInsertEvent.class.isAssignableFrom(eventType) ||
               PreUpdateEvent.class.isAssignableFrom(eventType);
    }

    private boolean invokeEvent(String eventName, PersistentEntity entity, EntityAccess ea) {
        final Map<String, Method> events = entityEvents.get(entity);
        if (events != null) {
            final Method eventMethod = events.get(eventName);
            if (eventMethod != null) {
                final Object result = ReflectionUtils.invokeMethod(eventMethod, ea.getEntity());
                boolean booleanResult = (result instanceof Boolean) ? (Boolean)result : true;
                if (booleanResult) {
                    ea.refresh();
                }
                return booleanResult;
            }
        }
        return true;
    }

    private void createEventCaches(PersistentEntity entity) {
        Class javaClass = entity.getJavaClass();
        final ConcurrentHashMap<String, Method> events = new ConcurrentHashMap<String, Method>();
        entityEvents.put(entity, events);

        findAndCacheEvent(EVENT_BEFORE_INSERT, javaClass, events);
        findAndCacheEvent(EVENT_BEFORE_UPDATE, javaClass, events);
        findAndCacheEvent(EVENT_BEFORE_DELETE, javaClass, events);
    }

    private void findAndCacheEvent(String event, Class javaClass, Map<String, Method> events) {
        final Method method = ReflectionUtils.findMethod(javaClass, event);
        if (method != null) {
            events.put(event, method);
        }
    }
}
