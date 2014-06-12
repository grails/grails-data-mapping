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
 * AWS DynamoDB template. This is a low-level way of accessing DynamoDB,
 * currently is uses AWS SDK API as the return and parameter types.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
public interface DynamoDBTemplate {
    /**
     * Returns null if not found
     * @param tableName complete name of the table in DynamoDB, will be used as-is
     * @param key the key for which to retrieve the data
     * @return null if the item is not found
     * @throws DataAccessException
     */
    Map<String,AttributeValue> get(String tableName, Key key) throws DataAccessException;

    /**
     * Same as get but with consistent read flag.
     * @param tableName complete name of the table in DynamoDB, will be used as-is
     * @param key the key for which to retrieve the data
     * @return null if the item is not found
     * @throws org.springframework.dao.DataAccessException
     */
    Map<String,AttributeValue> getConsistent(String tableName, Key key) throws DataAccessException;

    /**
     * Executes 'put' Dynamo DB command, replacing all existing attributes if they exist.
     *
     * http://docs.amazonwebservices.com/amazondynamodb/latest/developerguide/LowLevelJavaItemCRUD.html#PutLowLevelAPIJava
     *
     * @param tableName complete name of the table in DynamoDB, will be used as-is
     * @param attributes
     * @throws org.springframework.dao.DataAccessException
     */
    void putItem(String tableName, Map<String, AttributeValue> attributes) throws DataAccessException;

    /**
     * Executes 'put' Dynamo DB command, replacing all existing attributes if they exist.
     * Put is conditioned on the specified version - used for optimistic
     * locking. If the specified expectedVersion does not match what is in
     * dynamoDB, exception is thrown and no changes are made to the dynamoDB
     *
     * http://docs.amazonwebservices.com/amazondynamodb/latest/developerguide/LowLevelJavaItemCRUD.html#PutLowLevelAPIJava
     * @param tableName complete name of the table in DynamoDB, will be used as-is
     * @param key
     * @param attributes
     * @param expectedVersion
     * @throws org.springframework.dao.DataAccessException
     */
    void putItemVersioned(String tableName, Key key, Map<String, AttributeValue> attributes, String expectedVersion, PersistentEntity persistentEntity) throws DataAccessException;

    /**
     * Executes 'update' Dynamo DB command, which can be used to add/replace/delete specified attributes.
     *
     * http://docs.amazonwebservices.com/amazondynamodb/latest/developerguide/LowLevelJavaItemCRUD.html#LowLevelJavaItemUpdate
     * @param tableName complete name of the table in DynamoDB, will be used as-is
     * @param key
     * @param attributes
     * @throws org.springframework.dao.DataAccessException
     */
    void updateItem(String tableName, Key key, Map<String, AttributeValueUpdate> attributes) throws DataAccessException;

    /**
     * Executes 'update' Dynamo DB command, which can be used to add/replace/delete specified attributes.
     * Update is conditioned on the specified version - used for optimistic
     * locking. If the specified expectedVersion does not match what is in
     * dynamoDB, exception is thrown and no changes are made to the dynamoDB
     *
     * http://docs.amazonwebservices.com/amazondynamodb/latest/developerguide/LowLevelJavaItemCRUD.html#LowLevelJavaItemUpdate
     * @param tableName complete name of the table in DynamoDB, will be used as-is
     * @param key
     * @param attributes
     * @throws org.springframework.dao.DataAccessException
     */
    void updateItemVersioned(String tableName, Key key, Map<String, AttributeValueUpdate> attributes, String expectedVersion, PersistentEntity persistentEntity) throws DataAccessException;

    /**
     * Deletes the specified item with all of its attributes.
     *
     * @param tableName complete name of the table in DynamoDB, will be used as-is
     * @param key the key for which to retrieve the data
     */
    void deleteItem(String tableName, Key key) throws DataAccessException;

    /**
     * Returns true if any item was deleted, in other words if domain was empty it returns false.
     * @param tableName complete name of the table in DynamoDB, will be used as-is
     * @return true if any item was deleted
     * @throws org.springframework.dao.DataAccessException
     */
    boolean deleteAllItems(String tableName) throws DataAccessException;

    /**
     * Executes scan Dynamo DB operation (note this operation does not scale well with the growth of the table).
     * @param max maximum amount of items to return (inclusive)
     * @return the scan results
     * @throws org.springframework.dao.DataAccessException
     */
    List<Map<String, AttributeValue>> scan(String tableName, Map<String, Condition> filter, int max) throws DataAccessException;

    /**
     * Executes scan Dynamo DB operation and returns the count of matched items
     * (note this operation does not scale well with the growth of the table)
     * @param tableName the table name
     * @param filter filters
     * @return the count of matched items
     */
    int scanCount(String tableName, Map<String, Condition> filter);

    /**
     * Blocking call - internally will wait until the table is successfully deleted.
     * @throws DataAccessException
     */
    void deleteTable(String domainName) throws DataAccessException;

    List<String> listTables() throws DataAccessException;

    /**
     * Blocking call - internally will wait until the table is successfully created and is in ACTIVE state.
     * @param tableName the table name
     * @param ks the schema
     * @param provisionedThroughput the throughput
     * @throws DataAccessException
     */
    void createTable(String tableName, KeySchema ks, ProvisionedThroughput provisionedThroughput) throws DataAccessException;

    /**
     * Returns table description object containing throughput and key scheme information
     * @param tableName the table name
     * @return the description
     * @throws org.springframework.dao.DataAccessException
     */
    TableDescription describeTable(String tableName) throws DataAccessException;
}
