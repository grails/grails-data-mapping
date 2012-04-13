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
package org.grails.datastore.mapping.dynamodb.engine;

import java.util.LinkedList;
import java.util.List;

/**
 * An implementation of the table resolver which assumes there is no sharding -
 * i.e. always the same table name for all the primary keys (for the same type
 * of {@link org.grails.datastore.mapping.model.PersistentEntity}
 */
public class ConstDynamoDBTableResolver extends AbstractDynamoDBTableResolver {

    private List<String> tables;

    public ConstDynamoDBTableResolver(String entityFamily, String tableNamePrefix) {
        super(entityFamily, tableNamePrefix); //parent contains the logic for figuring out the final entityFamily
        tables = new LinkedList<String>();
        tables.add(getEntityFamily()); // without sharding there is just one table
    }

    public String resolveTable(String id) {
        return entityFamily; // without sharding it is always the same one per PersistentEntity
    }

    public List<String> getAllTablesForEntity() {
        return tables;
    }
}
