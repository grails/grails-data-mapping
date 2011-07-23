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
package org.grails.datastore.mapping.simpledb.util;

import com.amazonaws.services.simpledb.util.SimpleDBUtils;

import java.util.Collection;

/**
 * Simple util class for SimpleDB.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
public class SimpleDBUtil {
    /**
     * Quotes and escapes an attribute name or domain name by wrapping it with backticks and escaping any backticks inside the name.
     * @param name
     * @return
     */
    public static String quoteName(String name) {
        return SimpleDBUtils.quoteName(name);
    }

    /**
     * Quotes and escapes an attribute value by wrapping it with single quotes and escaping any single quotes inside the value.
     * @param value
     * @return
     */
    public static String quoteValue(String value){
        return SimpleDBUtils.quoteValue(value);
    }

    /**
     * Quotes and escapes a list of values so that they can be used in a SimpleDB query.
     * @param values
     * @return
     */
    public static String quoteValues(Collection<String> values){
        return SimpleDBUtils.quoteValues(values);
    }
}