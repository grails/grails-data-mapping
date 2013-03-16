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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.grails.datastore.mapping.dynamodb.DynamoDBDatastore;
import org.grails.datastore.mapping.dynamodb.config.DynamoDBDomainClassMappedForm;
import org.grails.datastore.mapping.model.ClassMapping;
import org.grails.datastore.mapping.model.PersistentEntity;

import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.Condition;
import com.amazonaws.services.dynamodb.model.Key;
import com.amazonaws.services.dynamodb.model.KeySchema;
import com.amazonaws.services.dynamodb.model.KeySchemaElement;
import com.amazonaws.services.dynamodb.model.ProvisionedThroughput;

/**
 * Simple util class for DynamoDB.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
public class DynamoDBUtil {
    public static final String AWS_ERR_CODE_CONDITIONAL_CHECK_FAILED = "ConditionalCheckFailedException";
    public static final String AWS_ERR_CODE_RESOURCE_NOT_FOUND = "ResourceNotFoundException";
    public static final int AWS_STATUS_CODE_SERVICE_UNAVAILABLE = 503;

    /**
     * If tableNamePrefix is not null returns prefixed table name.
     *
     * @param tableName
     * @param tableNamePrefix
     * @return
     */
    public static String getPrefixedTableName(String tableNamePrefix, String tableName) {
        if (tableNamePrefix != null) {
            return tableNamePrefix + tableName;
        }
        return tableName;
    }

    /**
     * Returns mapped table name (*unprefixed*) for the specified @{link PersistentEntity}.
     *
     * @param entity
     * @return
     */
    public static String getMappedTableName(PersistentEntity entity) {
        @SuppressWarnings("unchecked")
        ClassMapping<DynamoDBDomainClassMappedForm> classMapping = entity.getMapping();
        DynamoDBDomainClassMappedForm mappedForm = classMapping.getMappedForm();
        String entityFamily = getFamily(entity, mappedForm);
        return entityFamily;
    }

    private static String getFamily(PersistentEntity persistentEntity, DynamoDBDomainClassMappedForm mappedForm) {
        String table = null;
        if (mappedForm != null) {
            table = mappedForm.getFamily();
        }
        if (table == null) {
            table = persistentEntity.getJavaClass().getSimpleName();
        }
        return table;
    }

    /**
     * Returns ProvisionedThroughput for the specific entity, or uses default one if the entity does not define it
     *
     * @param entity
     * @param datastore
     * @return
     */
    public static ProvisionedThroughput getProvisionedThroughput(PersistentEntity entity, DynamoDBDatastore datastore) {
        @SuppressWarnings("unchecked")
        ClassMapping<DynamoDBDomainClassMappedForm> classMapping = entity.getMapping();
        DynamoDBDomainClassMappedForm mappedForm = classMapping.getMappedForm();

        Map<String, Object> throughput = mappedForm.getThroughput();

        if (throughput == null || throughput.isEmpty()) {
            return DynamoDBUtil.createDefaultProvisionedThroughput(datastore);
        }

        Number read = (Number) throughput.get(DynamoDBConst.THROUGHPUT_READ_ATTRIBUTE_NAME);
        if (read == null) {
            read = datastore.getDefaultReadCapacityUnits(); // default value
        }

        Number write = (Number) throughput.get(DynamoDBConst.THROUGHPUT_WRITE_ATTRIBUTE_NAME);
        if (write == null) {
            write = datastore.getDefaultWriteCapacityUnits(); // default value
        }

        ProvisionedThroughput provisionedThroughput = new ProvisionedThroughput().
                withReadCapacityUnits(read.longValue()).
                withWriteCapacityUnits(write.longValue());
        return provisionedThroughput;
    }

    /**
     * Returns KeySchema for the specific entity.
     *
     * @param entity
     * @param datastore
     * @return
     */
    public static KeySchema getKeySchema(PersistentEntity entity, DynamoDBDatastore datastore) {
        return DynamoDBUtil.createIdKeySchema(); //current implementation does not handle composite keys //TODO
    }

    public static String getAttributeValue(Map<String, AttributeValue> item, String attributeName) {
        AttributeValue av = item.get(attributeName);
        if (av != null) {
            return av.getS();
        }
        return null;
    }

    public static String getAttributeValueNumeric(Map<String, AttributeValue> item, String attributeName) {
        AttributeValue av = item.get(attributeName);
        if (av != null) {
            return av.getN();
        }
        return null;
    }

    public static List<String> getAttributeValues(Map<String, AttributeValue> item, String attributeName) {
        AttributeValue av = item.get(attributeName);
        if (av != null) {
            return av.getSS();
        }
        return null;
    }

    public static List<String> collectIds(List<Map<String, AttributeValue>> items) {
        if (items.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> ids = new LinkedList<String>();
        for (Map<String, AttributeValue> item : items) {
            ids.add(getAttributeValue(item, "id"));
        }
        return ids;
    }

    /**
     * Used in case we need to re-submit request to AWS when it throws 'AWS Error Code: ServiceUnavailable, AWS Error Message: Service AmazonDynamoDB is currently unavailable. Please try again '
     *
     * @param attemptNumber
     */
    public static void sleepBeforeRetry(int attemptNumber) {
        long sleepMS;
        if (attemptNumber < 5) {
            sleepMS = 100;
        } else if (attemptNumber < 10) {
            sleepMS = 1000;
        } else if (attemptNumber < 15) {
            sleepMS = 5000;
        } else if (attemptNumber < 20) {
            sleepMS = 30000;
        } else {
            sleepMS = 60000;
        }
        try {
            Thread.sleep(sleepMS);
        } catch (InterruptedException e) {
        }
    }

    public static Key getIdKey(Map<String, AttributeValue> item) {
        //todo - this currently works with non-ranged keys only
        return new Key(item.get("id"));
    }

    public static Key createIdKey(String id) {
        return new Key(new AttributeValue("id").withS(id));
    }

    public static KeySchema createIdKeySchema() {
        KeySchemaElement hashKey = new KeySchemaElement().withAttributeName("id").withAttributeType("S");
        KeySchema ks = new KeySchema().withHashKeyElement(hashKey);
        return ks;
    }

    public static ProvisionedThroughput createDefaultProvisionedThroughput(DynamoDBDatastore datastore) {
        ProvisionedThroughput provisionedThroughput = new ProvisionedThroughput().
                withReadCapacityUnits(datastore.getDefaultReadCapacityUnits()).
                withWriteCapacityUnits(datastore.getDefaultWriteCapacityUnits());
        return provisionedThroughput;
    }

    public static void addId(Map<String, AttributeValue> item, String id) {
        item.put("id", new AttributeValue().withS(id));
    }

    public static void addAttributeValue(Collection<AttributeValue> attributeValues, String stringValue, boolean isNumber) {
        attributeValues.add(createAttributeValue(stringValue, isNumber));
    }

    public static AttributeValue createAttributeValue(String stringValue, boolean isNumber) {
        if (isNumber) {
            return new AttributeValue().withN(stringValue);
        } else {
            return new AttributeValue().withS(stringValue);
        }
    }

    /*
       when toCombinate is [
                       [ [a],[b] ],
                       [ [c],[d] ]
                    ]
       returns [ [a,c], [a,d], [b,c], [b,d] ]
    */
    public static <T> List<List<T>> combinate(List<List<List<T>>> toCombinate) {
        if (toCombinate.isEmpty()) {
            return Collections.emptyList();
        } else if (toCombinate.size() == 1) {
            return toCombinate.get(0);
        } else {
            return recursiveCombine(toCombinate, 0);
        }
    }

    private static <T> List<List<T>> recursiveCombine(List<List<List<T>>> toCombinate, int index) {
        if (index == toCombinate.size() - 1) {
            //this is leaf, just return what we got at this position
            return new ArrayList<List<T>>(toCombinate.get(index));
        } else {
            List<List<T>> next = recursiveCombine(toCombinate, index + 1);
            //now combinate with each element in my list
            List<List<T>> result = new ArrayList<List<T>>();
            List<List<T>> currentContainer = toCombinate.get(index); //[ [a],[b] ]
            for (List<T> current : currentContainer) {
                for (List<T> n : next) {
                    List<T> temp = new ArrayList<T>(n);
                    temp.addAll(current);
                    result.add(temp);
                }
            }
            return result;
        }
    }

    /**
     * @param filter
     * @param key
     * @param operator
     * @param stringValue
     * @param isNumber
     */
    public static void addSimpleComparison(Map<String, Condition> filter, String key, String operator, String stringValue, boolean isNumber) {
        checkFilterForExistingKey(filter, key);
        if (isNumber) {
            filter.put(key, new Condition().withComparisonOperator(operator).withAttributeValueList(new AttributeValue().withN(stringValue)));
        } else {
            filter.put(key, new Condition().withComparisonOperator(operator).withAttributeValueList(new AttributeValue().withS(stringValue)));
        }
    }

    public static void checkFilterForExistingKey(Map<String, Condition> filter, String key) {
        if (filter.containsKey(key)) {
            throw new IllegalArgumentException("DynamoDB allows only a single filter condition per attribute. You are trying to use more than one condition for attribute: " + key);
        }
    }
}
