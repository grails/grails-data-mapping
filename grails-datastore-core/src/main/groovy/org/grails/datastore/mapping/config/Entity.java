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
package org.grails.datastore.mapping.config;

/**
 * Base class for classes returned from {@link org.grails.datastore.mapping.model.ClassMapping#getMappedForm()}
 *
 * @author Graeme Rocher
 * @since 1.1.9
 */
public class Entity {

    public static final String ALL_DATA_SOURCES = "ALL";
    public static final String DEFAULT_DATA_SOURCE = "DEFAULT";

    private boolean stateless = false;
    private boolean autoTimestamp = true;
    private boolean autowire = true;
    private Object defaultSort = null;
    private boolean version = true;

    /**
     * @return Whether the entity is versioned
     */
    public boolean isVersioned() {
        return version;
    }

    public void setVersion(boolean version) {
        this.version = version;
    }

    /**
     * @return Whether the entity should be autowired
     */
    public boolean isAutowire() {
        return autowire;
    }

    public void setAutowire(boolean autowire) {
        this.autowire = autowire;
    }

    /**
     * @return Whether automatic time stamps should be applied to 'lastUpdate' and 'dateCreated' properties
     */
    public boolean isAutoTimestamp() {
        return autoTimestamp;
    }

    public void setAutoTimestamp(boolean autoTimestamp) {
        this.autoTimestamp = autoTimestamp;
    }

    /**
     * @return Whether the entity state should be held in the session or not
     */
    public boolean isStateless() {
        return stateless;
    }

    public void setStateless(boolean stateless) {
        this.stateless = stateless;
    }

    /**
     * @return The default sort order definition, could be a string or a map
     */
    public Object getSort() {
        return defaultSort;
    }

    public void setSort(Object defaultSort) {
        this.defaultSort = defaultSort;
    }
}
