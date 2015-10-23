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

import javax.persistence.EnumType;
import javax.persistence.FetchType;

/**
 * Base class for classes returned from {@link org.grails.datastore.mapping.model.PropertyMapping#getMappedForm()}
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class Property implements Cloneable {

    private boolean index = false;
    private boolean nullable = false;
    private FetchType fetchStrategy = FetchType.LAZY;
    private String targetName;
    private String generator;
    private String propertyName;
    private EnumType enumType;

    @Override
    public Property clone() throws CloneNotSupportedException {
        return (Property) super.clone();
    }

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
     * @return The name of the property this property mapping relates to
     */
    public String getName() {
        return propertyName;
    }

    public void setName(String propertyName) {
        this.propertyName = propertyName;
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

    /**
     * @return The strategy to use to fetch the property (lazy or eager)
     */
    public FetchType getFetchStrategy() {
        return fetchStrategy;
    }

    public void setFetchStrategy(FetchType fetchStrategy) {
        this.fetchStrategy = fetchStrategy;
    }

    /**
     * @return Whether the property is nullable
     */
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

    /**
     * @return The type of the enum, either ordinal or string
     */
    public String getEnumType() {
        return enumType.toString();
    }

    /**
     * @return The type of the enum, either ordinal or string
     */
    public EnumType getEnumTypeObject() {
        return enumType;
    }

    public void setEnumType(EnumType enumType) {
        this.enumType = enumType;
    }

    public void setEnumType(String enumType) {
        this.enumType = EnumType.valueOf(enumType.toUpperCase());
    }
}
