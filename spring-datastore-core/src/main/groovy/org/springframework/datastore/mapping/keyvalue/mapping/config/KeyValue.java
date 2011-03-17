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
package org.springframework.datastore.mapping.keyvalue.mapping.config;

import org.springframework.datastore.mapping.config.Property;

/**
 * <p>A KeyValue is a used to define the key used for a particular value</p>
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class KeyValue extends Property {

    public KeyValue() {
    }

    public KeyValue(String key) {
        setTargetName(key);
    }

    public String getKey() {
        return getTargetName();
    }

    public void setKey(String key) {
        setTargetName(key);
    }
}
