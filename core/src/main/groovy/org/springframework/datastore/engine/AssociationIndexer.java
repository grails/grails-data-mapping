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

import java.util.List;

/**
 * Responsible for creating indices for associations used in queries
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public interface AssociationIndexer<K, T> {



    /**
     * Creates an index queryable via the primary key
     *
     * @param primaryKey The primary key
     * @param foreignKeys The foreign keys
     */
    void index( K primaryKey, List<T> foreignKeys);


    /**
     * Queries the given primary key and returns the foreign keys
     *
     * @param primaryKey The primary key
     * @return The foreign keys
     */
    List<T> query(K primaryKey);
}
