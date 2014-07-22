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

    private boolean stateless = false;
    private boolean autoTimestamp = true;
    private Object defaultSort = null;

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
