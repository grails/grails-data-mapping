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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

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

    protected Map<String, Boolean> entitiesWithDateCreated = new ConcurrentHashMap<String, Boolean>();
    protected Map<String, Boolean> entitiesWithLastUpdated = new ConcurrentHashMap<String, Boolean>();
    protected Collection<String> uninitializedEntities = new ConcurrentLinkedQueue<String>();
    
    
    private TimestampProvider timestampProvider = new DefaultTimestampProvider();
    

    public AutoTimestampEventListener(final Datastore datastore) {
        super(datastore);

        MappingContext mappingContext = datastore.getMappingContext();
        initForMappingContext(mappingContext);
    }

    protected AutoTimestampEventListener(final MappingContext mappingContext) {
        super(null);

        initForMappingContext(mappingContext);
    }

    protected void initForMappingContext(MappingContext mappingContext) {
        for (PersistentEntity persistentEntity : mappingContext.getPersistentEntities()) {
            storeDateCreatedAndLastUpdatedInfo(persistentEntity);
        }

        mappingContext.addMappingContextListener(this);
    }

    @Override
    protected void onPersistenceEvent(final AbstractPersistenceEvent event) {
        if (event.getEntity() == null) return;

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
        final String name = entity.getName();
        initializeIfNecessary(entity, name);
        Class<?> dateCreatedType = null;
        Object timestamp = null;
        if (hasDateCreated(name)) {
            dateCreatedType = ea.getPropertyType(DATE_CREATED_PROPERTY);
            timestamp = timestampProvider.createTimestamp(dateCreatedType);
            ea.setProperty(DATE_CREATED_PROPERTY, timestamp);
        }
        if (hasLastUpdated(name)) {
            Class<?> lastUpdateType = ea.getPropertyType(LAST_UPDATED_PROPERTY);
            if(dateCreatedType == null || !lastUpdateType.isAssignableFrom(dateCreatedType)) {
                timestamp = timestampProvider.createTimestamp(lastUpdateType);
            }
            ea.setProperty(LAST_UPDATED_PROPERTY, timestamp);
        }
        return true;
    }

    private void initializeIfNecessary(PersistentEntity entity, String name) {
        if(uninitializedEntities.contains(name)) {
            storeDateCreatedAndLastUpdatedInfo(entity);
            uninitializedEntities.remove(name);
        }
    }

    public boolean beforeUpdate(PersistentEntity entity, EntityAccess ea) {
        if (hasLastUpdated(entity.getName())) {
            Class<?> lastUpdateType = ea.getPropertyType(LAST_UPDATED_PROPERTY);
            Object timestamp = timestampProvider.createTimestamp(lastUpdateType);
            ea.setProperty(LAST_UPDATED_PROPERTY, timestamp);
        }
        return true;
    }

    /**
     * Here for binary compatibility. Deprecated.
     *
     * @deprecated Use {@link #hasLastUpdated(String)} instead
     */
    @Deprecated
    protected boolean hasLastUpdated(PersistentEntity entity) {
        return hasLastUpdated(entity.getName());
    }

    protected boolean hasLastUpdated(String n) {
        return entitiesWithLastUpdated.containsKey(n) && entitiesWithLastUpdated.get(n);
    }

    /**
     * Here for binary compatibility. Deprecated.
     *
     * @deprecated Use {@link #hasDateCreated(String)} instead
     */
    @Deprecated
    protected boolean hasDateCreated(PersistentEntity entity) {
        return hasDateCreated(entity.getName());
    }

    protected boolean hasDateCreated(String n) {
        return entitiesWithDateCreated.containsKey(n)&& entitiesWithDateCreated.get(n);
    }

    protected void storeDateCreatedAndLastUpdatedInfo(PersistentEntity persistentEntity) {
        if(persistentEntity.isInitialized()) {

            ClassMapping<?> classMapping = persistentEntity.getMapping();
            Entity mappedForm = classMapping.getMappedForm();
            if(mappedForm == null || mappedForm.isAutoTimestamp()) {
                storeTimestampAvailability(entitiesWithDateCreated, persistentEntity, persistentEntity.getPropertyByName(DATE_CREATED_PROPERTY));
                storeTimestampAvailability(entitiesWithLastUpdated, persistentEntity, persistentEntity.getPropertyByName(LAST_UPDATED_PROPERTY));
            }
        }
        else {
            uninitializedEntities.add(persistentEntity.getName());
        }
    }

    protected void storeTimestampAvailability(Map<String, Boolean> timestampAvailabilityMap, PersistentEntity persistentEntity, PersistentProperty<?> property) {
        timestampAvailabilityMap.put(persistentEntity.getName(), property != null && timestampProvider.supportsCreating(property.getType()));
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

    private void processAllEntries(final Set<Map.Entry<String, Boolean>> entries, final Runnable runnable)  {
        Map<String, Boolean> originalValues = new LinkedHashMap<String, Boolean>();
        for(Map.Entry<String, Boolean> entry: entries) {
            originalValues.put(entry.getKey(), entry.getValue());
            entry.setValue(false);
        }
        runnable.run();
        for(Map.Entry<String, Boolean> entry: entries) {
            entry.setValue(originalValues.get(entry.getKey()));
        }
    }

    private void processEntries(final List<Class> classes, Map<String, Boolean> entities, final Runnable runnable) {
        Set<Map.Entry<String, Boolean>> entries = new HashSet<>();
        final List<String> classNames = new ArrayList<>(classes.size());
        for(Class clazz: classes) {
            classNames.add(clazz.getName());
        }
        for (Map.Entry<String, Boolean> entry: entities.entrySet()) {
            if (classNames.contains(entry.getKey())) {
                entries.add(entry);
            }
        }
        processAllEntries(entries, runnable);
    }

    /**
     * Temporarily disables the last updated processing during the execution of the runnable
     * 
     * @param runnable The code to execute while the last updated listener is disabled
     */
    public void withoutLastUpdated(final Runnable runnable)  {
        processAllEntries(entitiesWithLastUpdated.entrySet(), runnable);
    }

    /**
     * Temporarily disables the last updated processing only on the provided classes during the execution of the runnable
     *
     * @param classes Which classes to disable the last updated processing for
     * @param runnable The code to execute while the last updated listener is disabled
     */
    public void withoutLastUpdated(final List<Class> classes, final Runnable runnable)  {
        processEntries(classes, entitiesWithLastUpdated, runnable);
    }

    /**
     * Temporarily disables the last updated processing only on the provided class during the execution of the runnable
     *
     * @param clazz Which class to disable the last updated processing for
     * @param runnable The code to execute while the last updated listener is disabled
     */
    public void withoutLastUpdated(final Class clazz, final Runnable runnable)  {
        ArrayList<Class> list = new ArrayList<Class>(1);
        list.add(clazz);
        withoutLastUpdated(list, runnable);
    }

    /**
     * Temporarily disables the date created processing during the execution of the runnable
     *
     * @param runnable The code to execute while the date created listener is disabled
     */
    public void withoutDateCreated(final Runnable runnable)  {
        processAllEntries(entitiesWithDateCreated.entrySet(), runnable);
    }

    /**
     * Temporarily disables the date created processing only on the provided classes during the execution of the runnable
     *
     * @param classes Which classes to disable the date created processing for
     * @param runnable The code to execute while the date created listener is disabled
     */
    public void withoutDateCreated(final List<Class> classes, final Runnable runnable)  {
        processEntries(classes, entitiesWithDateCreated, runnable);
    }

    /**
     * Temporarily disables the date created processing only on the provided class during the execution of the runnable
     *
     * @param clazz Which class to disable the date created processing for
     * @param runnable The code to execute while the date created listener is disabled
     */
    public void withoutDateCreated(final Class clazz, final Runnable runnable)  {
        ArrayList<Class> list = new ArrayList<Class>(1);
        list.add(clazz);
        withoutDateCreated(list, runnable);
    }

    /**
     * Temporarily disables the timestamp processing during the execution of the runnable
     *
     * @param runnable The code to execute while the timestamp listeners are disabled
     */
    public void withoutTimestamps(final Runnable runnable)  {
        withoutDateCreated(new Runnable() {
            @Override
            public void run() {
                withoutLastUpdated(runnable);
            }
        });
    }

    /**
     * Temporarily disables the timestamp processing only on the provided classes during the execution of the runnable
     *
     * @param classes Which classes to disable the timestamp processing for
     * @param runnable The code to execute while the timestamp listeners are disabled
     */
    public void withoutTimestamps(final List<Class> classes, final Runnable runnable)  {
        withoutDateCreated(classes, new Runnable() {
            @Override
            public void run() {
                withoutLastUpdated(classes, runnable);
            }
        });
    }

    /**
     * Temporarily disables the timestamp processing during the execution of the runnable
     *
     * @param clazz Which class to disable the timestamp processing for
     * @param runnable The code to execute while the timestamp listeners are disabled
     */
    public void withoutTimestamps(final Class clazz, final Runnable runnable)  {
        withoutDateCreated(clazz, new Runnable() {
            @Override
            public void run() {
                withoutLastUpdated(clazz, runnable);
            }
        });
    }

}
