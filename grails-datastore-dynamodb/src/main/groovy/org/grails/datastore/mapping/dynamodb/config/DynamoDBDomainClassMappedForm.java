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
package org.grails.datastore.mapping.dynamodb.config;

import java.util.Map;

import org.grails.datastore.mapping.keyvalue.mapping.config.Family;

/**
 * Mapping for
 * {@link org.grails.datastore.mapping.dynamodb.config.DynamoDBPersistentEntity}
 * with the DynamoDB specific properties so that the following can be used in
 * the mapping:
 *
 * <pre>
 *      static mapping = {
 *          table 'Person'
 *          id_generator type:'hilo', maxLo:100  //optional, if not specified UUID is used
 *          throughput read:10, write:5 //optional, if not specified default values will be used
 *      }
 * </pre>
 * @author Roman Stepanenko
 * @since 0.1
 */
public class DynamoDBDomainClassMappedForm extends Family {

    protected String table;
    protected Map<String, Object> id_generator; //id generation configuration
    protected Map<String, Object> throughput; //throughput configuration

    public DynamoDBDomainClassMappedForm() {
    }

    public DynamoDBDomainClassMappedForm(String table) {
        this.table = table;
    }

    public DynamoDBDomainClassMappedForm(String keyspace, String table) {
        super(keyspace, table);
        this.table = table;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
        super.setFamily(table);
    }

    @Override
    public void setFamily(String family) {
        super.setFamily(family);
        table = family;
    }

    public Map<String, Object> getId_generator() {
        return id_generator;
    }

    public void setId_generator(Map<String, Object> id_generator) {
        this.id_generator = id_generator;
    }

    public Map<String, Object> getThroughput() {
        return throughput;
    }

    public void setThroughput(Map<String, Object> throughput) {
        this.throughput = throughput;
    }

    @Override
    public String toString() {
        return "DynamoDBDomainClassMappedForm{" +
                "table='" + table + '\'' +
                ", id_generator=" + id_generator +
                ", throughput=" + throughput +
                '}';
    }
}
