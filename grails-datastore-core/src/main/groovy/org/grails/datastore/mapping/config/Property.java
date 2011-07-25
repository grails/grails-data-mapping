/* Copyright (C) 2011 SpringSource
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

import javax.persistence.FetchType;

public class Property {

    private boolean index = false;
    private boolean nullable = false;
    private FetchType fetchStrategy = FetchType.LAZY;
    private String targetName;
    private String generator;

    /**
     * The target to map to, could be a database column, document attribute, or hash key
     *
     * @return The target name
     */
    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
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

    public FetchType getFetchStrategy() {
        return fetchStrategy;
    }

    public void setFetchStrategy(FetchType fetchStrategy) {
        this.fetchStrategy = fetchStrategy;
    }

    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    /**
     * Set the id generator name or class.
     * @param generator name or class
     */
    public void setGenerator(String generator) {
        this.generator = generator;
    }

    /**
     * Get the id generator.
     * @return the name or class
     */
    public String getGenerator() {
        return generator;
    }
}
