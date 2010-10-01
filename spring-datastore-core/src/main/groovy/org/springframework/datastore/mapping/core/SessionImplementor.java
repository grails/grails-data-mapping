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
package org.springframework.datastore.mapping.core;

import org.springframework.datastore.mapping.model.PersistentEntity;

import java.io.Serializable;
import java.util.Collection;

/**
 * Methods for the implementation of the {@link Session} interface to implement
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public interface SessionImplementor<T> {
    Collection<Runnable> getPendingInserts();

    Collection<Runnable> getPendingUpdates();

    Collection<Runnable> getPendingDeletes();

    void cacheEntry(PersistentEntity entity, Serializable key, T entry);

    T getCachedEntry(PersistentEntity entity, Serializable key);

    void addPostFlushOperation(Runnable runnable);
}
