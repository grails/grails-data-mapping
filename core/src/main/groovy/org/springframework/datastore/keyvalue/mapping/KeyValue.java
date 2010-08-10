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
package org.springframework.datastore.keyvalue.mapping;

import org.springframework.datastore.mapping.types.Fetch;

/**
 * <p>A KeyValue is a used to define the key used for a particular value</p>
 * 
 * @author Graeme Rocher
 * @since 1.0
 */
public class KeyValue {

    private String key;
    private boolean index = false;
    private Fetch fetchStrategy = Fetch.LAZY;

    public KeyValue() {
    }

    public KeyValue(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    /**
     * @return Whether this property is index
     */
    public boolean isIndex() {
        return index;
    }

    /**
     * Whether this property is index
     * @param index Sets whether to index the property or not
     */
    public void setIndex(boolean index) {
        this.index = index;
    }

    public Fetch getFetchStrategy() {
        return this.fetchStrategy;
    }

    public void setFetchStrategy(Fetch fetchStrategy) {
        this.fetchStrategy = fetchStrategy;
    }
}
