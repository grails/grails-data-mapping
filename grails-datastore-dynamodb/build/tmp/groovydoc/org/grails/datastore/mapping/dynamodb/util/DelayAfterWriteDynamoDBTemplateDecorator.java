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
package org.grails.datastore.mapping.dynamodb.util;

import java.util.List;
import java.util.Map;

import org.grails.datastore.mapping.model.PersistentEntity;
import org.springframework.dao.DataAccessException;

import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodb.model.Condition;
import com.amazonaws.services.dynamodb.model.Key;
import com.amazonaws.services.dynamodb.model.KeySchema;
import com.amazonaws.services.dynamodb.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodb.model.TableDescription;

/**
 * Simple decorator used in testing to fight eventual consistency of DynamoDB.
 */
public class DelayAfterWriteDynamoDBTemplateDecorator implements DynamoDBTemplate {

    private DynamoDBTemplate template;
    private long delayMillis;

    public DelayAfterWriteDynamoDBTemplateDecorator(DynamoDBTemplate template, long delayMillis) {
        this.template = template;
        this.delayMillis = delayMillis;
    }

    public boolean deleteAllItems(String domainName) throws DataAccessException {
        boolean result = template.deleteAllItems(domainName);
        if (result) {
            pause(); //pause only if there were items to delete
        }
        return result;
    }

    public List<Map<String, AttributeValue>> scan(String tableName, Map<String, Condition> filter, int max) throws DataAccessException {
        return template.scan(tableName, filter, max);
    }

    public int scanCount(String tableName, Map<String, Condition> filter) {
        return template.scanCount(tableName, filter);
    }

    public void deleteTable(String domainName) throws DataAccessException {
        template.deleteTable(domainName);
        pause();
    }

    public Map<String,AttributeValue> get(String tableName, Key id) throws DataAccessException {
        return template.get(tableName, id);
    }

    public Map<String,AttributeValue> getConsistent(String domainName, Key id) throws DataAccessException {
        return template.getConsistent(domainName, id);
    }

    public void putItem(String tableName, Map<String, AttributeValue> attributes) throws DataAccessException {
        template.putItem(tableName, attributes);
//        pause();      //for tests we use DelayAfterWriteDynamoDBSession which pauses after flush
    }

    public void putItemVersioned(String tableName, Key key, Map<String, AttributeValue> attributes, String expectedVersion, PersistentEntity persistentEntity) throws DataAccessException {
        template.putItemVersioned(tableName, key, attributes, expectedVersion, persistentEntity);
//        pause();      //for tests we use DelayAfterWriteDynamoDBSession which pauses after flush
    }

    public void updateItem(String tableName, Key key, Map<String, AttributeValueUpdate> attributes) throws DataAccessException {
        template.updateItem(tableName, key, attributes);
//        pause();      //for tests we use DelayAfterWriteDynamoDBSession which pauses after flush
    }

    public void updateItemVersioned(String tableName, Key key, Map<String, AttributeValueUpdate> attributes, String expectedVersion, PersistentEntity persistentEntity) throws DataAccessException {
        template.updateItemVersioned(tableName, key, attributes, expectedVersion, persistentEntity);
//        pause();      //for tests we use DelayAfterWriteDynamoDBSession which pauses after flush
    }

    public void deleteItem(String tableName, Key key) throws DataAccessException {
        template.deleteItem(tableName, key);
//        pause();      //for tests we use DelayAfterWriteDynamoDBSession which pauses after flush
    }

    public List<String> listTables() throws DataAccessException {
        return template.listTables();
    }

    public void createTable(String tableName, KeySchema ks, ProvisionedThroughput provisionedThroughput) throws DataAccessException {
        template.createTable(tableName, ks, provisionedThroughput);
        pause();
    }

    public TableDescription describeTable(String tableName) throws DataAccessException {
        return template.describeTable(tableName);
    }

    private void pause() {
        try { Thread.sleep(delayMillis); } catch (InterruptedException e) { /* ignored */ }
    }
}
