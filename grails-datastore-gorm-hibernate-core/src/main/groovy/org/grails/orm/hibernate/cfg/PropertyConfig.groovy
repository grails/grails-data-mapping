/*
 * Copyright 2003-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.orm.hibernate.cfg

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.config.Property
import org.hibernate.FetchMode

import javax.persistence.FetchType

/**
 * Custom mapping for a single domain property. Note that a property
 * can have multiple columns via a component or a user type.
 *
 * @since 1.0.4
 * @author pledbrook
 */
@CompileStatic
class PropertyConfig extends Property {

    boolean explicitSaveUpdateCascade;

    /**
     * The Hibernate type or user type of the property. This can be
     * a string or a class.
     */
    def type

    /**
     * The parameters for the property that can be used to
     * configure a Hibernate ParameterizedType implementation.
     */
    Properties typeParams

    /**
     * The default sort property name
     */
    String sort


    /**
     * The default sort order
     */
    String order

    /**
     * The batch size used for lazy loading
     */
    Integer batchSize


    /**
     * Whether to ignore ObjectNotFoundException
     */
    boolean ignoreNotFound = false

    /**
    * Whether or not this is column is insertable by hibernate
     */
    boolean insertable = true

    /**
    * Whether or not this column is updateable by hibernate
     */
    boolean updateable = true

    List<ColumnConfig> columns = []

    CacheConfig cache
    JoinTable joinTable = new JoinTable()

    void setFetch(FetchMode fetch) {
        if(FetchMode.JOIN.equals(fetch)) {
            super.setFetchStrategy(FetchType.EAGER)
        }
        this.fetch = fetch
    }

    FetchMode getFetch() {
        return fetch
    }
    /**
     * The column used to produce the index for index based collections (lists and maps)
     */
    PropertyConfig indexColumn

    /**
     * Shortcut to get the column name for this property.
     * @throws RuntimeException if this property maps to more than one
     * column.
     */
    String getColumn() {
        checkHasSingleColumn()
        if(columns.isEmpty()) return null
        return columns[0].name
    }

    String getEnumType() {
        checkHasSingleColumn()
        if(columns.isEmpty()) return "default"
        return columns[0].enumType
    }

    /**
     * Shortcut to get the SQL type of the corresponding column.
     * @throws RuntimeException if this property maps to more than one
     * column.
     */
    String getSqlType() {
        checkHasSingleColumn()
        if(columns.isEmpty()) return null
        return columns[0].sqlType
    }

    /**
     * Shortcut to get the index setting for this property's column.
     * @throws RuntimeException if this property maps to more than one
     * column.
     */
    String getIndex() {
        checkHasSingleColumn()
        if(columns.isEmpty()) return null
        return columns[0].index?.toString()
    }

    /**
     * Shortcut to determine whether the property's column is configured
     * to be unique.
     * @throws RuntimeException if this property maps to more than one
     * column.
     */
    boolean isUnique() {
        if(columns.size()>1) {
            return super.isUnique()
        }
        else {
            if(columns.isEmpty()) return super.isUnique()
            return columns[0].unique
        }
    }

    /**
     * Shortcut to get the length of this property's column.
     * @throws RuntimeException if this property maps to more than one
     * column.
     */
    int getLength() {
        checkHasSingleColumn()
        if(columns.isEmpty()) return -1
        return columns[0].length
    }

    /**
     * Shortcut to get the precision of this property's column.
     * @throws RuntimeException if this property maps to more than one
     * column.
     */
    int getPrecision() {
        checkHasSingleColumn()
        if(columns.isEmpty()) return -1
        return columns[0].precision
    }

    /**
     * Shortcut to get the scale of this property's column.
     * @throws RuntimeException if this property maps to more than one
     * column.
     */
    int getScale() {
        checkHasSingleColumn()
        if(columns.isEmpty()) {
            return super.getScale()
        }
        return columns[0].scale
    }

    @Override
    void setScale(int scale) {
        checkHasSingleColumn()
        if(!columns.isEmpty())  {
            columns[0].scale = scale
        }
        else {
            super.setScale(scale)
        }
    }

    String toString() {
        "property[type:$type, lazy:$lazy, columns:$columns, insertable:${insertable}, updateable:${updateable}]"
    }

    protected void checkHasSingleColumn() {
        if (columns?.size() > 1) {
            throw new RuntimeException("Cannot treat multi-column property as a single-column property")
        }
    }

    @Override
    PropertyConfig clone() throws CloneNotSupportedException {
        PropertyConfig pc = (PropertyConfig)super.clone()

        pc.fetch = fetch
        pc.indexColumn = indexColumn != null ? (PropertyConfig)indexColumn.clone() : null
        pc.cache = cache != null ? cache.clone() : cache
        pc.joinTable = joinTable.clone()

        def newColumns = new ArrayList<>(columns.size())
        pc.columns = newColumns
        for(c in columns) {
            newColumns.add(c.clone())
        }
        return pc
    }
}
