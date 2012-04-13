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

import org.grails.datastore.mapping.dynamodb.util.DynamoDBUtil;

/**
 * @author Roman Stepanenko
 */
public abstract class AbstractDynamoDBTableResolver implements DynamoDBTableResolver {

    protected String entityFamily;
    protected String tableNamePrefix;

    public AbstractDynamoDBTableResolver(String entityFamily, String tableNamePrefix) {
        this.tableNamePrefix = tableNamePrefix;
        this.entityFamily = DynamoDBUtil.getPrefixedTableName(tableNamePrefix, entityFamily);
    }

    /**
     * Helper getter for subclasses.
     * @return entityFamily
     */
    protected String getEntityFamily(){
        return entityFamily;
    }
}
