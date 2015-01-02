/*
 * Copyright 2014 the original author or authors.
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
package org.grails.datastore.gorm

/**
 * 
 * @author Jeff Brown
 * @since 4.0
 */
trait PersistenceMethods<D> {
    
    static GormInstanceApi internalApi
    
    static void initInternalApi(GormInstanceApi api) {
        internalApi = api
    }
    /**
     * Proxy aware instanceOf implementation.
     */
    boolean instanceOf(Class cls) {
        internalApi.instanceOf this, cls
    }

    /**
     * Upgrades an existing persistence instance to a write lock
     * @return The instance
     */
    D lock() {
        internalApi.lock this
    }

    /**
     * Locks the instance for updates for the scope of the passed closure
     *
     * @param callable The closure
     * @return The result of the closure
     */
    def mutex(Closure callable) {
        internalApi.mutex this, callable
    }

    /**
     * Refreshes the state of the current instance
     * @return The instance
     */
    D refresh() {
        internalApi.refresh this
    }

    /**
     * Saves an object the datastore
     * @return Returns the instance
     */
    D save() {
        internalApi.save this
    }

    /**
     * Forces an insert of an object to the datastore
     * @return Returns the instance
     */
    D insert() {
        internalApi.insert this
    }

    /**
     * Forces an insert of an object to the datastore
     * @return Returns the instance
     */
    D insert(Map params) {
        internalApi.insert this, params
    }

    /**
     * Saves an object the datastore
     * @return Returns the instance
     */
    D merge() {
        internalApi.merge this
    }

    /**
     * Saves an object the datastore
     * @return Returns the instance
     */
    D merge(Map params) {
        internalApi.merge this, params
    }

    /**
     * Save method that takes a boolean which indicates whether to perform validation or not
     *
     * @param validate Whether to perform validation
     *
     * @return The instance or null if validation fails
     */
    D save(boolean validate) {
        internalApi.save this, validate
    }

    /**
     * Saves an object with the given parameters
     * @param instance The instance
     * @param params The parameters
     * @return The instance
     */
    D save(Map params) {
        internalApi.save this, params
    }

    /**
     * Returns the objects identifier
     */
    Serializable ident() {
        internalApi.ident this
    }

    /**
     * Attaches an instance to an existing session. Requries a session-based model
     * @return
     */
    D attach() {
        internalApi.attach this
    }

    /**
     * No concept of session-based model so defaults to true
     */
    boolean isAttached() {
        internalApi.isAttached this
    }

    /**
     * Discards any pending changes. Requires a session-based model.
     */
    void discard() {
        internalApi.discard this
    }

    /**
     * Deletes an instance from the datastore
     */
    void delete() {
        internalApi.delete this
    }

    /**
     * Deletes an instance from the datastore
     */
    void delete(Map params) {
        internalApi.delete this, params
    }

    /**
     * Checks whether a field is dirty
     *
     * @param instance The instance
     * @param fieldName The name of the field
     *
     * @return true if the field is dirty
     */
    boolean isDirty(String fieldName) {
        internalApi.isDirty this, fieldName
    }

    /**
     * Checks whether an entity is dirty
     *
     * @param instance The instance
     * @return true if it is dirty
     */
    boolean isDirty() {
        internalApi.isDirty this
    }

    /**
     * Obtains a list of property names that are dirty
     *
     * @param instance The instance
     * @return A list of property names that are dirty
     */
    List getDirtyPropertyNames() {
        internalApi.getDirtyPropertyNames this
    }

    /**
     * Gets the original persisted value of a field.
     *
     * @param fieldName The field name
     * @return The original persisted value
     */
    Object getPersistentValue(String fieldName) {
        internalApi.getPersistentValue this, fieldName
    }
}
