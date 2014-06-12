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
package org.grails.datastore.mapping.simple;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.grails.datastore.mapping.core.AbstractDatastore;
import org.grails.datastore.mapping.core.Session;
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext;
import org.grails.datastore.mapping.model.MappingContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * A simple implementation of the {@link org.grails.datastore.mapping.core.Datastore} interface that backs onto an in-memory map.
 * Mainly used for mocking and testing scenarios.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings("rawtypes")
public class SimpleMapDatastore extends AbstractDatastore {
    private Map<String, Map> datastore = new ConcurrentHashMap<String, Map>();
    private Map indices = new ConcurrentHashMap();

    /**
     * Creates a map based datastore backing onto the specified map
     *
     * @param datastore The datastore to back on to
     * @param ctx the application context
     */
    public SimpleMapDatastore(Map<String, Map> datastore, ConfigurableApplicationContext ctx) {
        this();
        this.datastore = datastore;
        setApplicationContext(ctx);
    }

    /**
     * Creates a map based datastore for the specified mapping context
     *
     * @param mappingContext The mapping context
     */
    public SimpleMapDatastore(MappingContext mappingContext, ConfigurableApplicationContext ctx) {
        super(mappingContext, null, ctx);
        initializeConverters(getMappingContext());
    }

    public SimpleMapDatastore() {
        this(null);
    }

    public SimpleMapDatastore(ConfigurableApplicationContext ctx) {
        this(new KeyValueMappingContext(""), ctx);
    }

    public Map getIndices() {
        return indices;
    }

    @Override
    protected Session createSession(Map<String, String> connectionDetails) {
        return new SimpleMapSession(this, getMappingContext(), getApplicationEventPublisher());
    }

    public Map<String, Map> getBackingMap() {
        return datastore;
    }

    public void clearData() {
        datastore.clear();
        indices.clear();
    }
}
