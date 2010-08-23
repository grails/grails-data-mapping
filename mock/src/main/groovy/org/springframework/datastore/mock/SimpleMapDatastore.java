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
package org.springframework.datastore.mock;

import org.springframework.datastore.core.AbstractDatastore;
import org.springframework.datastore.core.Session;
import org.springframework.datastore.keyvalue.mapping.KeyValueMappingContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple implementation of the {@link org.springframework.datastore.core.Datastore} interface that backs onto an in-memory map. 
 * Mainly used for mocking and testing scenarios
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class SimpleMapDatastore extends AbstractDatastore{
    private Map<String, Map> datastore = new ConcurrentHashMap<String, Map>();

    /**
     * Creates a map based datastore backing onto the specified map
     *
     * @param datastore The datastore to back on to
     */
    public SimpleMapDatastore(Map<String, Map> datastore) {
        this.datastore = datastore;
    }

    public SimpleMapDatastore() {
    }

    @Override
    protected Session createSession(Map<String, String> connectionDetails) {
        return new SimpleMapSession(this, new KeyValueMappingContext(""));
    }

    public Map<String, Map> getBackingMap() {
        return datastore;
    }
}
