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

import org.grails.datastore.mapping.model.PersistentProperty;

/**
 * Interface for defining custom datastore types beyond the simple and association types supported out of the box
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public interface CustomTypeMarshaller<T, N> {

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
     * @return The converted type
     */
    public Object convert(PersistentProperty property,  T value);

    /**
     * Converts a value from its native form
     * @param property The property being converted
     * @param source The native form
     * @return The converted type
     */
    public T read(PersistentProperty property, N source);
}
