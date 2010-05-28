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
package org.grails.inconsequential.kv.mapping;

/**
 * @author Graeme Rocher
 * @since 1.0
 */
public class KeyValueMapping  {
    
    private String keyspace;
    private String family;

    public KeyValueMapping(String keyspace, String family) {
        this.keyspace = keyspace;
        this.family = family;
    }

    public String getKeyspace() {
        return keyspace;
    }

    public String getFamily() {
        return family;
    }
}
