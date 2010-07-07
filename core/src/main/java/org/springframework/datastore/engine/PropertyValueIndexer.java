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
 * Responsible for creating indices for property values used in queries
 *
 * This interface is designed for usage in datastores that don't automatically
 * create indices and require the application to create the indices manually
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public interface PropertyValueIndexer<K> {

    /**
     * Creates an index for the given value to the specified key
     *
     * @param value The value
     * @param primaryKey The key
     */
    void index(Object value, K primaryKey);

    /**
     * Queries the given value and returns the keys
     *
     * @return The primary keys
     */
    List<K> query(Object value);
}
