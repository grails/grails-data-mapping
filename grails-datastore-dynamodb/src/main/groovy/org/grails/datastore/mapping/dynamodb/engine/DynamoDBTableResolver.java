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

import java.util.List;

/**
 * Encapsulates logic of determining DynamoDB domain name based specific a
 * primary key, assuming that this instance of the resolver is used only for one
 * {@link org.grails.datastore.mapping.model.PersistentEntity}, which
 * was provided during construction time of this instance.
 *
 * Strictly speaking sharding is not really needed for DynamoDB because it is supposed
 * to provide infinite scalability of a single table, but is left just in case.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
public interface DynamoDBTableResolver {

    /**
     * Returns domain name for the specified primary key value.
     *
     * @param id
     * @return
     */
    String resolveTable(String id);

    /**
     * Returns all domain names for this type of entity. Without sharding this
     * list contains always one element.
     *
     * @return
     */
    List<String> getAllTablesForEntity();
}
