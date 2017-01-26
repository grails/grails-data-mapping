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

import groovy.transform.AutoClone

/**
 * Defines a column within the mapping.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@AutoClone
class ColumnConfig {
    /**
     * The column name
     */
    String name
    /**
     * The SQL type
     */
    String sqlType
    /**
     * The enum type
     */
    String enumType = 'default'
    /**
     * The index, can be either a boolean or a string for the name of the index
     */
    def index
    /**
     * Whether the column is unique
     */
    boolean unique = false
    /**
     * The length of the column
     */
    int length = -1
    /**
     * The precision of the column
     */
    int precision = -1
    /**
     * The scale of the column
     */
    int scale = -1
    /**
     * The default value
     */
    String defaultValue
    /**
     * A comment to apply to the column
     */
    String comment
    /**
     * A custom read string
     */
    String read
    /**
     * A custom write sstring
     */
    String write

    String toString() {
        "column[name:$name, index:$index, unique:$unique, length:$length, precision:$precision, scale:$scale]"
    }
}
