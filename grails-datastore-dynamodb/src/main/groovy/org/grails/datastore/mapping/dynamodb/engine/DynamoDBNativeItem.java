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

import com.amazonaws.services.dynamodb.model.AttributeValue;
import org.grails.datastore.mapping.dynamodb.util.DynamoDBUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Logical representation of how information is loaded from and sent to AWS.
 * <p/>
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
public class DynamoDBNativeItem {

    private Map<String, AttributeValue> data = new HashMap<String, AttributeValue>(); //todo - not sure about concurrency requirements?

    public DynamoDBNativeItem() {
    }

    public DynamoDBNativeItem(Map<String, AttributeValue> item) {
        //populate map with the item attributes. //todo - handle multi-value attributes/long string etc
        for (Map.Entry<String, AttributeValue> entry : item.entrySet()) {
            data.put(entry.getKey(), entry.getValue());
        }
    }

    public void put(String key, String stringValue, boolean isNumber) {
        data.put(key, DynamoDBUtil.createAttributeValue(stringValue, isNumber));
    }

    public String get(String key) {
        AttributeValue attributeValue = data.get(key);
        if (attributeValue == null) {
            return null;
        }

        //it can be either numeric or string format, try first string and then numeric - unfortunately we do not have access to real type
        String result = attributeValue.getS();
        if (result == null) {
            result = attributeValue.getN();
        }

        return result;
    }

    public Map<String, AttributeValue> createItem() {
//        Map<String, AttributeValue> result = new HashMap<String, AttributeValue>();
//        result.putAll(data);
//        return result;
        return data; //this method is used only in read-only fashion, so it is safe to return the inner map
    }

    @Override
    public String toString() {
        return "DynamoDBNativeItem{data=" + data + '}';
    }
}
