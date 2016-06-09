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

import groovy.lang.IntRange;
import groovy.lang.ObjectRange;

import javax.persistence.EnumType;
import javax.persistence.FetchType;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for classes returned from {@link org.grails.datastore.mapping.model.PropertyMapping#getMappedForm()}
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class Property implements Cloneable {

    private boolean index = false;
    private boolean nullable = false;
    private boolean unique = false;
    private FetchType fetchStrategy = FetchType.LAZY;
    private Boolean lazy = null;
    private String targetName;
    private String generator;
    private String propertyName;
    private EnumType enumType;
    private Number maxSize = null;
    private Number minSize = null;
    private Comparable max = null;
    private Comparable min = null;
    private int scale = -1;
    private List<String> inList = null;
    private List<String> uniquenessGroup = new ArrayList<String>();
    private boolean derived;
    private String cascade;
    private String formula;
    @Override
    public Property clone() throws CloneNotSupportedException {
        return (Property) super.clone();
    }
    /**
     * The formula used to build the property
     */
    public String getFormula() {
        return formula;
    }

    public void setFormula(String formula) {
        this.formula = formula;
    }

    /**
     * Cascading strategy for this property. Only makes sense if the
     * property is an association or collection.
     */
    public String getCascade() {
        return cascade;
    }

    public void setCascade(String cascade) {
        this.cascade = cascade;
    }

    /**
     * @return Whether the property is derived or not
     */
    public boolean isDerived() {
        return formula != null || derived;
    }

    public void setDerived(boolean derived) {
        this.derived = derived;
    }

    /**
     * @return The maximum value
     */
    public Comparable getMax() {
        return max;
    }

    public void setMax(Comparable max) {
        this.max = max;
    }

    /**
     *
     * @return The minimum value
     */
    public Comparable getMin() {
        return min;
    }

    public void setMin(Comparable min) {
        this.min = min;
    }

    public void setRange(ObjectRange range) {
        if(range != null) {
            this.max = range.getTo();
            this.min = range.getFrom();
        }
    }

    /**
     * @return The scale
     */
    public int getScale() {
        return scale;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    /**
     * @return List of valid values
     */
    public List<String> getInList() {
        return inList;
    }

    public void setInList(List<String> inList) {
        this.inList = inList;
    }

    /**
     * @return The maximum size
     */
    public Number getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(Number maxSize) {
        this.maxSize = maxSize;
    }


    public void setSize(IntRange maxSize) {
        this.maxSize = maxSize.getTo();
        this.minSize = maxSize.getFrom();
    }

    /**
     * @return The minimum size
     */
    public Number getMinSize() {
        return minSize;
    }

    public void setMinSize(Number minSize) {
        this.minSize = minSize;
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

    /**
     * Sets the strategy to use to fetch the property (lazy or eager)
     *
     * @param fetchStrategy The fetch strategy to use
     */
    public void setFetchStrategy(FetchType fetchStrategy) {
        this.fetchStrategy = fetchStrategy;
    }

    /**
     * Makes it easier to configure the fetch strategy
     *
     * @param name The name of the fetch strategy
     */
    public void setFetch(String name) {
        if(FetchType.EAGER.name().equalsIgnoreCase(name)) {
            setFetchStrategy(FetchType.EAGER);
        }
        else {
            setFetchStrategy(FetchType.LAZY);
        }
    }

    /**
     * Whether to use lazy proxies for each association. This has no effect if {@link #getFetchStrategy()} returns FetchType.EAGER, however
     * if FetchType is LAZY and lazy is set to true then for collection types each element of the collection will be a proxy.
     *
     * If lazy is false the collection will be fetched lazily, but fully initialized objects will be loaded for each element.
     *
     * @return Whether to use lazy proxies for collection elements
     */
    public boolean isLazy() {
        return lazy == Boolean.TRUE;
    }

    public Boolean getLazy() {
        return lazy;
    }

    /**
     * @param lazy Set to true if lazy proxies should be used for each element of collection types
     */
    public void setLazy(Boolean lazy) {
        this.lazy = lazy;
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
     * @return Whether the property should be unique
     */
    public boolean isUnique() {
        return unique;
    }

    /**
     * @return Whether the property is unique within a group
     */
    public boolean isUniqueWithinGroup() {
        return !uniquenessGroup.isEmpty();
    }


    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    public void setUnique(String name) {
        this.unique = true;
        this.uniquenessGroup.add(name);
    }

    public void setUnique(List<String> names) {
        this.unique = true;
        this.uniquenessGroup.addAll(names);
    }

    public List<String> getUniquenessGroup() {
        return uniquenessGroup;
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
