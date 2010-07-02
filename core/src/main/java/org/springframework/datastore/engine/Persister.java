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

import org.springframework.datastore.mapping.MappingContext;

import java.io.Serializable;
import java.util.List;

/**
 * A Persister is responsible for persisting and retrieving an object.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public interface Persister {

    /**
     * The type this persister persists
     *
     * @return The class this persisters persists
     */
    Class getType();

    /**
     * Persist an object using the given mapping context
     *
     * @param context The MappingContext
     * @param obj The object
     * @return A generated Key
     */
    Serializable persist(MappingContext context, Object obj);

    /**
     * Persists a number of objects at the same time and
     * returns their keys in the order specified by the objs parameter
     *
     * @param context The context
     * @param objs The objects
     * @return A list of keys
     */
    List<Serializable> persist(MappingContext context, Iterable objs);

    /**
     * Retrieves an object for the given context and Key
     *
     * @param context The context
     * @param key The key
     * 
     * @return The object in question
     */
    Object retrieve(MappingContext context, Serializable key);

    /**
     * Deletes one or many objects
     * @param context The context
     * @param objects The objects to delete. Must all be of the same type or an exception will be thrown.
     */
    void delete(MappingContext context, Iterable objects);

    /**
     * Batch retrieve several objects in one go
     *
     * @param context The context
     * @param keys The keys
     * @return The objects in a list in the same order as the specified keys
     */
    List<Object> retrieveAll(MappingContext context, Iterable<Serializable> keys);

    /**
     * Deletes a single object
     * @param mappingContext The MappingContext
     * @param obj The object
     */
    void delete(MappingContext mappingContext, Object obj);
}
