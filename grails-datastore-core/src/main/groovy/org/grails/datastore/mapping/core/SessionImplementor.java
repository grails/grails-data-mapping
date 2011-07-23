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
package org.grails.datastore.mapping.core;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import org.grails.datastore.mapping.core.impl.PendingInsert;
import org.grails.datastore.mapping.core.impl.PendingUpdate;
import org.grails.datastore.mapping.model.PersistentEntity;

/**
 * Methods for the implementation of the {@link Session} interface to implement.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings("rawtypes")
public interface SessionImplementor<T> {

    void addPendingInsert(PendingInsert insert);

    void addPendingUpdate(PendingUpdate update);

    Map<PersistentEntity, Collection<PendingInsert>> getPendingInserts();

    Map<PersistentEntity, Collection<PendingUpdate>> getPendingUpdates();

    Collection<Runnable> getPendingDeletes();

    void cacheEntry(PersistentEntity entity, Serializable key, T entry);

    T getCachedEntry(PersistentEntity entity, Serializable key);
    T getCachedEntry(PersistentEntity entity, Serializable key, boolean forDirtyCheck);

    void cacheInstance(Class type, Serializable key, Object instance);

    /**
     * Get the cached instance if it exists.
     * @param type the object type
     * @param key the object key
     * @return the instance or <code>null</code>
     */
    Object getCachedInstance(Class type, Serializable key);

    /**
     * Whether an object with the specified key is contained within the first level cache.
     * @param type the object type
     * @param key The key to check
     * @return <code>true</code> if it is
     */
    boolean isCached(Class type, Serializable key);

    Collection getCachedCollection(PersistentEntity entity, Serializable key, String name);

    void cacheCollection(PersistentEntity entity, Serializable key, Collection collection, String name);

    void addPostFlushOperation(Runnable runnable);
}
