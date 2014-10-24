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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.grails.datastore.gorm.timestamp.DefaultTimestampProvider;
import org.grails.datastore.gorm.timestamp.TimestampProvider;
import org.grails.datastore.mapping.config.Entity;
import org.grails.datastore.mapping.core.Datastore;
import org.grails.datastore.mapping.engine.EntityAccess;
import org.grails.datastore.mapping.engine.event.AbstractPersistenceEvent;
import org.grails.datastore.mapping.engine.event.AbstractPersistenceEventListener;
import org.grails.datastore.mapping.engine.event.EventType;
import org.grails.datastore.mapping.engine.event.PreInsertEvent;
import org.grails.datastore.mapping.engine.event.PreUpdateEvent;
import org.grails.datastore.mapping.model.ClassMapping;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.springframework.context.ApplicationEvent;

/**
 * An event listener that adds support for GORM-style auto-timestamping
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class AutoTimestampEventListener extends AbstractPersistenceEventListener implements MappingContext.Listener {

    public static final String DATE_CREATED_PROPERTY = "dateCreated";
    public static final String LAST_UPDATED_PROPERTY = "lastUpdated";

    protected Map<PersistentEntity, Boolean> entitiesWithDateCreated = new ConcurrentHashMap<PersistentEntity, Boolean>();
    protected Map<PersistentEntity, Boolean> entitiesWithLastUpdated = new ConcurrentHashMap<PersistentEntity, Boolean>();
    
    
    private TimestampProvider timestampProvider = new DefaultTimestampProvider();
    

    public AutoTimestampEventListener(final Datastore datastore) {
        super(datastore);

        for (PersistentEntity persistentEntity : datastore.getMappingContext().getPersistentEntities()) {
            storeDateCreatedAndLastUpdatedInfo(persistentEntity);
        }

        datastore.getMappingContext().addMappingContextListener(this);
    }

    @Override
    protected void onPersistenceEvent(final AbstractPersistenceEvent event) {
        if (event.getEventType() == EventType.PreInsert) {
            beforeInsert(event.getEntity(), event.getEntityAccess());
        }
        else if (event.getEventType() == EventType.PreUpdate) {
            beforeUpdate(event.getEntity(), event.getEntityAccess());
        }
    }

    public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        return PreInsertEvent.class.isAssignableFrom(eventType) ||
               PreUpdateEvent.class.isAssignableFrom(eventType);
    }

    public boolean beforeInsert(PersistentEntity entity, EntityAccess ea) {
        Class<?> dateCreatedType = null;
        Object timestamp = null;
        if (hasDateCreated(entity)) {
            dateCreatedType = ea.getPropertyType(DATE_CREATED_PROPERTY);
            timestamp = timestampProvider.createTimestamp(dateCreatedType);
            ea.setProperty(DATE_CREATED_PROPERTY, timestamp);
        }
        if (hasLastupdated(entity)) {
            Class<?> lastUpdateType = ea.getPropertyType(LAST_UPDATED_PROPERTY);
            if(dateCreatedType == null || !lastUpdateType.isAssignableFrom(dateCreatedType)) {
                timestamp = timestampProvider.createTimestamp(lastUpdateType);
            }
            ea.setProperty(LAST_UPDATED_PROPERTY, timestamp);
        }
        return true;
    }
    
    public boolean beforeUpdate(PersistentEntity entity, EntityAccess ea) {
        if (hasLastupdated(entity)) {
            Class<?> lastUpdateType = ea.getPropertyType(LAST_UPDATED_PROPERTY);
            Object timestamp = timestampProvider.createTimestamp(lastUpdateType);
            ea.setProperty(LAST_UPDATED_PROPERTY, timestamp);
        }
        return true;
    }

    protected boolean hasLastupdated(PersistentEntity entity) {
        return entitiesWithLastUpdated.containsKey(entity) && entitiesWithLastUpdated.get(entity);
    }

    protected boolean hasDateCreated(PersistentEntity entity) {
        return entitiesWithDateCreated.containsKey(entity)&& entitiesWithDateCreated.get(entity);
    }

    protected void storeDateCreatedAndLastUpdatedInfo(PersistentEntity persistentEntity) {
        ClassMapping<?> classMapping = persistentEntity.getMapping();
        Entity mappedForm = classMapping.getMappedForm();
        if(mappedForm == null || mappedForm.isAutoTimestamp()) {
            storeTimestampAvailability(entitiesWithDateCreated, persistentEntity, persistentEntity.getPropertyByName(DATE_CREATED_PROPERTY));
            storeTimestampAvailability(entitiesWithLastUpdated, persistentEntity, persistentEntity.getPropertyByName(LAST_UPDATED_PROPERTY));
        }
    }

    protected void storeTimestampAvailability(Map<PersistentEntity, Boolean> timestampAvailabilityMap, PersistentEntity persistentEntity, PersistentProperty<?> property) {
        timestampAvailabilityMap.put(persistentEntity, property != null ? timestampProvider.supportsCreating(property.getType()) : false);
    }

    public void persistentEntityAdded(PersistentEntity entity) {
        storeDateCreatedAndLastUpdatedInfo(entity);
    }

    public TimestampProvider getTimestampProvider() {
        return timestampProvider;
    }

    public void setTimestampProvider(TimestampProvider timestampProvider) {
        this.timestampProvider = timestampProvider;
    }
}
