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

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodb.AmazonDynamoDB;
import com.amazonaws.services.dynamodb.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodb.model.*;
import org.grails.datastore.mapping.core.OptimisticLockingException;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.springframework.dao.DataAccessException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * Implementation of DynamoDBTemplate using AWS Java SDK.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
public class DynamoDBTemplateImpl implements DynamoDBTemplate {

    private AmazonDynamoDB ddb;

    public DynamoDBTemplateImpl(AmazonDynamoDB ddb) {
        this.ddb = ddb;
    }

    public DynamoDBTemplateImpl(String accessKey, String secretKey) {
        Assert.isTrue(StringUtils.hasLength(accessKey) && StringUtils.hasLength(secretKey),
                "Please provide accessKey and secretKey");

        ddb = new AmazonDynamoDBClient(new BasicAWSCredentials(accessKey, secretKey));
    }

    public Map<String, AttributeValue> get(String tableName, Key id) {
        return getInternal(tableName, id, 1);
    }

    private Map<String, AttributeValue> getInternal(String tableName, Key key, int attempt) {
        GetItemRequest request = new GetItemRequest(tableName, key);
        try {
            GetItemResult result = ddb.getItem(request);
            Map<String, AttributeValue> attributes = result.getItem();
            if (attributes == null || attributes.isEmpty()) {
                return null;
            }
            return attributes;
        } catch (AmazonServiceException e) {
            if (DynamoDBUtil.AWS_ERR_CODE_RESOURCE_NOT_FOUND.equals(e.getErrorCode())) {
                throw new IllegalArgumentException("no such table: " + tableName, e);
            } else if (DynamoDBUtil.AWS_STATUS_CODE_SERVICE_UNAVAILABLE == e.getStatusCode()) {
                //retry after a small pause
                DynamoDBUtil.sleepBeforeRetry(attempt);
                attempt++;
                return getInternal(tableName, key, attempt);
            } else {
                throw new DataStoreOperationException("problem with table: " + tableName + ", key: " + key, e);
            }
        }
    }

    public Map<String, AttributeValue> getConsistent(String domainName, Key key) {
        return getConsistentInternal(domainName, key, 1);
    }

    private Map<String, AttributeValue> getConsistentInternal(String tableName, Key key, int attempt) {
        GetItemRequest request = new GetItemRequest(tableName, key);
        request.setConsistentRead(true);
        try {
            GetItemResult result = ddb.getItem(request);
            Map<String, AttributeValue> attributes = result.getItem();
            if (attributes == null || attributes.isEmpty()) {
                return null;
            }
            return attributes;
        } catch (AmazonServiceException e) {
            if (DynamoDBUtil.AWS_ERR_CODE_RESOURCE_NOT_FOUND.equals(e.getErrorCode())) {
                throw new IllegalArgumentException("no such table: " + tableName, e);
            } else if (DynamoDBUtil.AWS_STATUS_CODE_SERVICE_UNAVAILABLE == e.getStatusCode()) {
                //retry after a small pause
                DynamoDBUtil.sleepBeforeRetry(attempt);
                attempt++;
                return getConsistentInternal(tableName, key, attempt);
            } else {
                throw new DataStoreOperationException("problem with table: " + tableName + ", key: " + key, e);
            }
        }
    }

    /**
     * Executes 'put' Dynamo DB command, replacing all existing attributes if they exist.
     *
     * @param tableName  complete name of the table in DynamoDB, will be used as-is
     * @param attributes
     * @throws DataAccessException
     */
    @Override
    public void putItem(String tableName, Map<String, AttributeValue> attributes) throws DataAccessException {
        putItemInternal(tableName, attributes, 1);
    }

    private void putItemInternal(String tableName, Map<String, AttributeValue> attributes, int attempt) throws DataAccessException {
        try {
            PutItemRequest request = new PutItemRequest(tableName, attributes);
            ddb.putItem(request);
        } catch (AmazonServiceException e) {
            if (DynamoDBUtil.AWS_ERR_CODE_RESOURCE_NOT_FOUND.equals(e.getErrorCode())) {
                throw new IllegalArgumentException("no such table: " + tableName, e);
            } else if (DynamoDBUtil.AWS_STATUS_CODE_SERVICE_UNAVAILABLE == e.getStatusCode()) {
                //retry after a small pause
                DynamoDBUtil.sleepBeforeRetry(attempt);
                attempt++;
                putItemInternal(tableName, attributes, attempt);
            } else {
                throw new DataStoreOperationException("problem with table: " + tableName + ", attributes: " + attributes, e);
            }
        }
    }

    /**
     * Executes 'put' Dynamo DB command, replacing all existing attributes if they exist.
     * Put is conditioned on the specified version - used for optimistic
     * locking. If the specified expectedVersion does not match what is in
     * dynamoDB, exception is thrown and no changes are made to the dynamoDB
     *
     * @param tableName       complete name of the table in DynamoDB, will be used as-is
     * @param key
     * @param attributes
     * @param expectedVersion
     * @throws DataAccessException
     */
    @Override
    public void putItemVersioned(String tableName, Key key, Map<String, AttributeValue> attributes, String expectedVersion, PersistentEntity persistentEntity) throws DataAccessException {
        putItemVersionedInternal(tableName, key, attributes, expectedVersion, persistentEntity, 1);
    }

    private void putItemVersionedInternal(String tableName, Key key, Map<String, AttributeValue> attributes, String expectedVersion, PersistentEntity persistentEntity, int attempt) throws DataAccessException {
        PutItemRequest request = new PutItemRequest(tableName, attributes).withExpected(getOptimisticVersionCondition(expectedVersion));
        try {
            ddb.putItem(request);
        } catch (AmazonServiceException e) {
            if (DynamoDBUtil.AWS_ERR_CODE_CONDITIONAL_CHECK_FAILED.equals(e.getErrorCode())) {
                throw new OptimisticLockingException(persistentEntity, key);
            } else if (DynamoDBUtil.AWS_ERR_CODE_RESOURCE_NOT_FOUND.equals(e.getErrorCode())) {
                throw new IllegalArgumentException("no such table: " + tableName, e);
            } else if (DynamoDBUtil.AWS_STATUS_CODE_SERVICE_UNAVAILABLE == e.getStatusCode()) {
                //retry after a small pause
                DynamoDBUtil.sleepBeforeRetry(attempt);
                attempt++;
                putItemVersionedInternal(tableName, key, attributes, expectedVersion, persistentEntity, attempt);
            } else {
                throw new DataStoreOperationException("problem with table: " + tableName + ", key: " + key + ", attributes: " + attributes, e);
            }
        }
    }

    /**
     * Executes 'update' Dynamo DB command, which can be used to add/replace/delete specified attributes.
     *
     * @param tableName  complete name of the table in DynamoDB, will be used as-is
     * @param key
     * @param attributes
     * @throws DataAccessException
     */
    @Override
    public void updateItem(String tableName, Key key, Map<String, AttributeValueUpdate> attributes) throws DataAccessException {
        updateItemInternal(tableName, key, attributes, 1);
    }

    private void updateItemInternal(String tableName, Key key, Map<String, AttributeValueUpdate> attributes, int attempt) throws DataAccessException {
        try {
            UpdateItemRequest request = new UpdateItemRequest(tableName, key, attributes);
            ddb.updateItem(request);
        } catch (AmazonServiceException e) {
            if (DynamoDBUtil.AWS_ERR_CODE_RESOURCE_NOT_FOUND.equals(e.getErrorCode())) {
                throw new IllegalArgumentException("no such table: " + tableName, e);
            } else if (DynamoDBUtil.AWS_STATUS_CODE_SERVICE_UNAVAILABLE == e.getStatusCode()) {
                //retry after a small pause
                DynamoDBUtil.sleepBeforeRetry(attempt);
                attempt++;
                updateItemInternal(tableName, key, attributes, attempt);
            } else {
                throw new DataStoreOperationException("problem with table: " + tableName + ", key: " + key + ", attributes: " + attributes, e);
            }
        }
    }

    /**
     * Executes 'update' Dynamo DB command, which can be used to add/replace/delete specified attributes.
     * Update is conditioned on the specified version - used for optimistic
     * locking. If the specified expectedVersion does not match what is in
     * dynamoDB, exception is thrown and no changes are made to the dynamoDB
     *
     * @param tableName  complete name of the table in DynamoDB, will be used as-is
     * @param key
     * @param attributes
     * @throws DataAccessException
     */
    @Override
    public void updateItemVersioned(String tableName, Key key, Map<String, AttributeValueUpdate> attributes, String expectedVersion, PersistentEntity persistentEntity) throws DataAccessException {
        updateItemVersionedInternal(tableName, key, attributes, expectedVersion, persistentEntity, 1);
    }

    private void updateItemVersionedInternal(String tableName, Key key, Map<String, AttributeValueUpdate> attributes, String expectedVersion, PersistentEntity persistentEntity, int attempt) throws DataAccessException {
        UpdateItemRequest request = new UpdateItemRequest(tableName, key, attributes).withExpected(getOptimisticVersionCondition(expectedVersion));
        try {
            ddb.updateItem(request);
        } catch (AmazonServiceException e) {
            if (DynamoDBUtil.AWS_ERR_CODE_CONDITIONAL_CHECK_FAILED.equals(e.getErrorCode())) {
                throw new OptimisticLockingException(persistentEntity, key);
            } else if (DynamoDBUtil.AWS_ERR_CODE_RESOURCE_NOT_FOUND.equals(e.getErrorCode())) {
                throw new IllegalArgumentException("no such table: " + tableName, e);
            } else if (DynamoDBUtil.AWS_STATUS_CODE_SERVICE_UNAVAILABLE == e.getStatusCode()) {
                //retry after a small pause
                DynamoDBUtil.sleepBeforeRetry(attempt);
                attempt++;
                updateItemVersionedInternal(tableName, key, attributes, expectedVersion, persistentEntity, attempt);
            } else {
                throw new DataStoreOperationException("problem with table: " + tableName + ", key: " + key + ", attributes: " + attributes, e);
            }
        }
    }

    public void deleteItem(String tableName, Key key) {
        deleteItemInternal(tableName, key, 1);
    }

    private void deleteItemInternal(String tableName, Key key, int attempt) {
        DeleteItemRequest request = new DeleteItemRequest(tableName, key);
        try {
            ddb.deleteItem(request);
        } catch (AmazonServiceException e) {
            if (DynamoDBUtil.AWS_ERR_CODE_RESOURCE_NOT_FOUND.equals(e.getErrorCode())) {
                throw new IllegalArgumentException("no such table: " + tableName, e);
            } else if (DynamoDBUtil.AWS_STATUS_CODE_SERVICE_UNAVAILABLE == e.getStatusCode()) {
                //retry after a small pause
                DynamoDBUtil.sleepBeforeRetry(attempt);
                attempt++;
                deleteItemInternal(tableName, key, attempt);
            } else {
                throw new DataStoreOperationException("problem with table: " + tableName + ", key: " + key, e);
            }
        }
    }

    public boolean deleteAllItems(String tableName) throws DataAccessException {
        ScanRequest request = new ScanRequest().withTableName(tableName);
        boolean deleted = false;
        ScanResult result = ddb.scan(request);
        for (Map<String, AttributeValue> item : result.getItems()) {
            Key key = DynamoDBUtil.getIdKey(item);
            deleteItem(tableName, key);
            deleted = true;
        }

        //keep repeating until we get through all matched items
        Key lastKeyEvaluated = null;
        do {
            lastKeyEvaluated = result.getLastEvaluatedKey();
            if (lastKeyEvaluated != null) {
                request = new ScanRequest(tableName).withExclusiveStartKey(lastKeyEvaluated);
                result = ddb.scan(request);
                for (Map<String, AttributeValue> item : result.getItems()) {
                    Key key = DynamoDBUtil.getIdKey(item);
                    deleteItem(tableName, key);
                    deleted = true;
                }
            }
        } while (lastKeyEvaluated != null);

        return deleted;
    }

    public List<Map<String, AttributeValue>> scan(String tableName, Map<String, Condition> filter, int max) {
        return scanInternal(tableName, filter, max, 1);
    }

    private List<Map<String, AttributeValue>> scanInternal(String tableName, Map<String, Condition> filter, int max, int attempt) {
        LinkedList<Map<String, AttributeValue>> items = new LinkedList<Map<String, AttributeValue>>();
        try {
            ScanRequest request = new ScanRequest(tableName).withScanFilter(filter);
            ScanResult result = ddb.scan(request);
            items.addAll(result.getItems());

            //keep repeating until we get through all matched items
            Key lastKeyEvaluated = null;
            do {
                lastKeyEvaluated = result.getLastEvaluatedKey();
                if (lastKeyEvaluated != null) {
                    request = new ScanRequest(tableName).withScanFilter(filter).withExclusiveStartKey(lastKeyEvaluated);
                    result = ddb.scan(request);
                    items.addAll(result.getItems());
                }
            } while (lastKeyEvaluated != null && items.size() < max);

            //truncate if needed
            while (items.size() > max) {
                items.removeLast();
            }

            return items;
        } catch (AmazonServiceException e) {
            if (DynamoDBUtil.AWS_ERR_CODE_RESOURCE_NOT_FOUND.equals(e.getErrorCode())) {
                throw new IllegalArgumentException("no such table: " + tableName, e);
            } else if (DynamoDBUtil.AWS_STATUS_CODE_SERVICE_UNAVAILABLE == e.getStatusCode()) {
                //retry after a small pause
                DynamoDBUtil.sleepBeforeRetry(attempt);
                attempt++;
                return scanInternal(tableName, filter, max, attempt);
            } else {
                throw new DataStoreOperationException("problem with table: " + tableName + ", filter: " + filter, e);
            }
        }
    }

    @Override
    public int scanCount(String tableName, Map<String, Condition> filter) {
        return scanCountInternal(tableName, filter, 1);
    }

    private int scanCountInternal(String tableName, Map<String, Condition> filter, int attempt) {
        LinkedList<Map<String, AttributeValue>> items = new LinkedList<Map<String, AttributeValue>>();
        try {
            ScanRequest request = new ScanRequest(tableName).withScanFilter(filter).withCount(true);
            ScanResult result = ddb.scan(request);
            int count = 0;
            count = count + result.getCount();

            //keep repeating until we get through all matched items
            Key lastKeyEvaluated = null;
            do {
                lastKeyEvaluated = result.getLastEvaluatedKey();
                if (lastKeyEvaluated != null) {
                    request = new ScanRequest(tableName).withScanFilter(filter).withExclusiveStartKey(lastKeyEvaluated).withCount(true);
                    result = ddb.scan(request);
                    count = count + result.getCount();
                }
            } while (lastKeyEvaluated != null);

            return count;
        } catch (AmazonServiceException e) {
            if (DynamoDBUtil.AWS_ERR_CODE_RESOURCE_NOT_FOUND.equals(e.getErrorCode())) {
                throw new IllegalArgumentException("no such table: " + tableName, e);
            } else if (DynamoDBUtil.AWS_STATUS_CODE_SERVICE_UNAVAILABLE == e.getStatusCode()) {
                //retry after a small pause
                DynamoDBUtil.sleepBeforeRetry(attempt);
                attempt++;
                return scanCountInternal(tableName, filter, attempt);
            } else {
                throw new DataStoreOperationException("problem with table: " + tableName + ", filter: " + filter, e);
            }
        }
    }

    @Override
    public void createTable(String tableName, KeySchema ks, ProvisionedThroughput provisionedThroughput) throws DataAccessException {
        CreateTableRequest request = new CreateTableRequest()
                .withTableName(tableName)
                .withKeySchema(ks)
                .withProvisionedThroughput(provisionedThroughput);

        try {
            CreateTableResult result = ddb.createTable(request);
            //now we must wait until table is ACTIVE
            TableDescription tableDescription = waitTillTableState(tableName, "ACTIVE");
            if (!"ACTIVE".equals(tableDescription.getTableStatus())) {
                throw new DataStoreOperationException("could not create table " + tableName + ", current table description: " + tableDescription);
            }
        } catch (AmazonClientException e) {
            throw new DataStoreOperationException("problem with table: " + tableName + ", key schema: " + ks + ", provisioned throughput: " + provisionedThroughput, e);
        }
    }

    public List<String> listTables() throws DataAccessException {
        ListTablesRequest request = new ListTablesRequest();
        try {
            ListTablesResult result = ddb.listTables(request);
            return result.getTableNames();
        } catch (AmazonClientException e) {
            throw new DataStoreOperationException("", e);
        }
    }

    /**
     * Returns table description object containing throughput and key scheme information
     * @param tableName
     * @return
     * @throws DataAccessException
     */
    @Override
    public TableDescription describeTable(String tableName) throws DataAccessException{
        TableDescription tableDescription = ddb.describeTable(new DescribeTableRequest().withTableName(tableName)).getTable();
        return tableDescription;
    }

    public void deleteTable(String tableName) throws DataAccessException {
        DeleteTableRequest request = new DeleteTableRequest(tableName);
        try {
            ddb.deleteTable(request);
            try {
                int attempt = 0;
                TableDescription tableDescription = null;
                do {
                    tableDescription = ddb.describeTable(new DescribeTableRequest().withTableName(tableName)).getTable();
                    attempt++;
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                    }
                } while (attempt < 1000);
                //if we got here it means there was no ResourceNotFoundException, so it was not deleted
                throw new DataStoreOperationException("could not delete table " + tableName + ", current table description: " + tableDescription);
            } catch (ResourceNotFoundException e) {
                //this is good, it means table is actually deleted
                return;
            }
        } catch (AmazonClientException e) {
            throw new DataStoreOperationException("problem with table: " + tableName, e);
        }
    }

    protected Map<String, ExpectedAttributeValue> getOptimisticVersionCondition(String expectedVersion) {
        Map<String, ExpectedAttributeValue> expectedMap = new HashMap<String, ExpectedAttributeValue>();
        expectedMap.put("version", new ExpectedAttributeValue(new AttributeValue().withN(expectedVersion)));
        return expectedMap;
    }

    private TableDescription waitTillTableState(String tableName, String desiredState) {
        int attempt = 0;
        TableDescription tableDescription = null;
        do {
            tableDescription = ddb.describeTable(new DescribeTableRequest().withTableName(tableName)).getTable();
            attempt++;
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
            }
        } while (attempt < 1000 && !desiredState.equals(tableDescription.getTableStatus()));
        return tableDescription;
    }
}
