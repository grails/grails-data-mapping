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
package org.grails.datastore.mapping.engine.event;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.grails.datastore.mapping.engine.EntityAccess;
import org.springframework.context.ApplicationEvent;
import org.grails.datastore.mapping.core.Datastore;
import org.grails.datastore.mapping.engine.BeanEntityAccess;
import org.grails.datastore.mapping.model.PersistentEntity;

/**
 * @author Burt Beckwith
 */
@SuppressWarnings("serial")
public abstract class AbstractPersistenceEvent extends ApplicationEvent {

    private final PersistentEntity entity;
    private final Object entityObject;
    private final EntityAccess entityAccess;
    private boolean cancelled;
    private List<String> excludedListenerNames = new ArrayList<String>();
    private Serializable nativeEvent;

    protected AbstractPersistenceEvent(final Datastore source, final PersistentEntity entity,
            final EntityAccess entityAccess) {
        super(source);
        this.entity = entity;
        this.entityAccess = entityAccess;
        if(entityAccess != null) {
            this.entityObject = entityAccess.getEntity();
        }
        else {
            this.entityObject = null;
        }
    }

    protected AbstractPersistenceEvent(final Datastore source, final Object entity) {
        super(source);
        entityObject = entity;
        this.entity = null;
        this.entityAccess = null;
    }

    public Object getEntityObject() {
        return entityObject;
    }

    public PersistentEntity getEntity() {
        return entity;
    }

    public EntityAccess getEntityAccess() {
        return entityAccess;
    }

    public void cancel() {
        cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void addExcludedListenerName(final String name) {
        excludedListenerNames.add(name);
    }

    public boolean isListenerExcluded(final String name) {
        return excludedListenerNames.contains(name);
    }

    public void setNativeEvent(final Serializable nativeEvent) {
        this.nativeEvent = nativeEvent;
    }

    public Serializable getNativeEvent() {
        return nativeEvent;
    }

    public abstract EventType getEventType();
}
