/* Copyright 2013 the original author or authors.
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
package org.grails.datastore.mapping.config

import groovy.transform.CompileStatic
import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy;
import org.grails.datastore.mapping.core.connections.ConnectionSource;

/**
 * Base class for classes returned from {@link org.grails.datastore.mapping.model.ClassMapping#getMappedForm()}
 *
 * @author Graeme Rocher
 * @since 1.1.9
 */
@CompileStatic
@Builder(builderStrategy = SimpleStrategy, prefix = '')
public class Entity {

    public static final String ALL_DATA_SOURCES = ConnectionSource.ALL
    public static final String DEFAULT_DATA_SOURCE = ConnectionSource.DEFAULT

    /**
     * @return Whether the entity state should be held in the session or not
     */
    boolean stateless = false;
    /**
     * @return Whether automatic time stamps should be applied to 'lastUpdate' and 'dateCreated' properties
     */
    boolean autoTimestamp = true
    /**
     * @return Whether the entity should be autowired
     */
    boolean autowire = false

    /**
     * @return The default sort order definition, could be a string or a map
     */
    Object defaultSort = null

    /**
     * @return Whether the entity is versioned
     */
    boolean version = true


    public Object getSort() {
        return defaultSort;
    }

    public Entity setSort(Object defaultSort) {
        this.defaultSort = defaultSort;
        return this
    }

    /**
     * Get the datasource names that this domain class works with.
     * @return the datasource names
     */
    List<String> datasources = [ ConnectionSource.DEFAULT ]

    /**
     * Sets the datastore to use
     *
     * @param name
     * @return
     */
    public Entity datasource(String name) {
        this.datasources = [name]
        return this
    }


    /**
     * Sets the datastore to use
     *
     * @param name
     * @return
     */
    public Entity connection(String name) {
        this.datasources = [name]
        return this
    }


    /**
     * Sets the datastore to use
     *
     * @param name
     * @return
     */
    public Entity connections(String...names) {
        if(names.length > 0) {
            this.datasources = Arrays.asList(names)
        }
        return this
    }

    /**
     * @return Whether this entity is versioned
     */
    boolean isVersioned() {
        return version
    }
}
