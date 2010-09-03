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
package org.springframework.datastore.engine;

import org.springframework.datastore.core.DatastoreAware;
import org.springframework.datastore.mapping.PersistentEntity;

/**
 * Used to intercept persistent operations
 *
 * @since 1.0
 * @author Graeme Rocher
 */
public interface EntityInterceptor extends DatastoreAware {

    /**
     * Called before an entity is inserted
     *
     * @param entity The entity
     * @param entityAccess The object
     * @return False if the operation should be cancelled
     */
    boolean beforeInsert(PersistentEntity entity, EntityAccess entityAccess);

    /**
     * Called before an entity is updated
     *
     * @param entity The entity
     * @param entityAccess The object
     * @return False if the operation should be cancelled
     */
    boolean beforeUpdate(PersistentEntity entity, EntityAccess entityAccess);

    /**
     * Called before an entity is deleted
     *
     * @param entity The entity
     * @param entityAccess The object
     * @return False if the operation should be cancelled
     */
    boolean beforeDelete(PersistentEntity entity, EntityAccess entityAccess);

    // TODO: Add more interception hooks

}
