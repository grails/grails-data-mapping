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
package org.grails.datastore.mapping.engine.types;

import org.grails.datastore.mapping.core.Datastore;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.query.Query;

/**
 * Interface for defining custom datastore types beyond the simple and association types supported out of the box
 *
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public interface CustomTypeMarshaller<T, N, Q> {

    /**
     * Whether the marshaller supports the given datastore type
     *
     * @param datastore The datastore type
     * @return True if it is supported
     */
    boolean supports(Datastore datastore);

    /**
     *
     * @return The target Java type
     */
    Class<T> getTargetType();

    /**
     * Converts a value to its native form
     *
     * @param property The property being converted
     * @param value The value
     * @param nativeTarget The nativeTarget
     */
    public void write(PersistentProperty property, T value, N nativeTarget);

    /**
     * Populates a query
     *
     * @param property The property being converted

     * @param criterion The criterion
     * @param nativeQuery The nativeQuery
     */
    public void query(PersistentProperty property,  Query.PropertyCriterion criterion, Q nativeQuery);

    /**
     * Converts a value from its native form
     * @param property The property being converted
     * @param source The native form
     * @return The converted type
     */
    public T read(PersistentProperty property, N source);
}
