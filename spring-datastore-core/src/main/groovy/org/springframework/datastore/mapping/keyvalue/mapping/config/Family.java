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

/**
 * <p>A Family is a grouping of KeyValue pairs and is typically composed
 * of a keyspace and the family name.</p>
 *
 * <p>For example in Cassandra a Family relates to a ColumnFamily or in BigTable
 * a Family relates to a an Entity kind.</p>
 *
 * <p>Other more simplistic key/value stores may just use prefixes on keys for the family</p>
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class Family {
    
    private String keyspace;
    private String family;

    public Family() {
    }

    public Family(String keyspace, String family) {
        this.keyspace = keyspace;
        this.family = family;
    }

    public String getKeyspace() {
        return keyspace;
    }

    public String getFamily() {
        return family;
    }

    public void setKeyspace(String keyspace) {
        this.keyspace = keyspace;
    }

    public void setFamily(String family) {
        this.family = family;
    }
}
