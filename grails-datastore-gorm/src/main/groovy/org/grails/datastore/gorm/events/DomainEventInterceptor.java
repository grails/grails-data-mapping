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

import org.springframework.datastore.mapping.core.Datastore;
import org.springframework.datastore.mapping.engine.EmptyInterceptor;
import org.springframework.datastore.mapping.engine.EntityAccess;
import org.springframework.datastore.mapping.model.MappingContext;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An interceptor that provides support for GORM domain events
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class DomainEventInterceptor extends EmptyInterceptor implements MappingContext.Listener {

    private Map<PersistentEntity, Map<String, Method>> entityEvents = new ConcurrentHashMap<PersistentEntity, Map<String, Method>>();
    public static final Class[] ZERO_PARAMS = new Class[0];
    public static final String EVENT_BEFORE_INSERT = "beforeInsert";
    private static final String EVENT_BEFORE_UPDATE = "beforeUpdate";
    private static final String EVENT_BEFORE_DELETE = "beforeDelete";

    @Override
    public boolean beforeInsert(PersistentEntity entity, EntityAccess ea) {
        return invokeEvent(EVENT_BEFORE_INSERT, entity, ea);
    }

    @Override
    public boolean beforeUpdate(PersistentEntity entity, EntityAccess ea) {
        return invokeEvent(EVENT_BEFORE_UPDATE, entity, ea);
    }

    @Override
    public boolean beforeDelete(PersistentEntity entity, EntityAccess ea) {
        return invokeEvent(EVENT_BEFORE_DELETE, entity, ea);
    }

    private boolean invokeEvent(String eventName, PersistentEntity entity, EntityAccess ea) {
        final Map<String, Method> events = entityEvents.get(entity);
        if(events != null) {
            final Method eventMethod = events.get(eventName);
            if(eventMethod != null) {
                final Object result = ReflectionUtils.invokeMethod(eventMethod, ea.getEntity());
                boolean booleanResult = (result instanceof Boolean) ? (Boolean)result : true;
                if(booleanResult) {
                    ea.refresh();
                }
                return booleanResult;

            }
        }
        return true;
    }

    @Override
    public void setDatastore(Datastore datastore) {
        super.setDatastore(datastore);

        for (PersistentEntity entity : datastore.getMappingContext().getPersistentEntities()) {
            createEventCaches(entity);
        }

        datastore.getMappingContext().addMappingContextListener(this);
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
        if(method != null) {
            events.put(event, method);
        }
    }

    public void persistentEntityAdded(PersistentEntity entity) {
        createEventCaches(entity);
    }
}
