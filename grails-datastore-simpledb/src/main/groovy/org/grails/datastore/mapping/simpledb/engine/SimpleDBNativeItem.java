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
package org.grails.datastore.mapping.simpledb.engine;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import com.amazonaws.services.simpledb.model.ReplaceableItem;

/**
 * Logical representation of how information is loaded from and sent to AWS.
 * <p/>
 * It stores all data in an internal Map and then creates appropriate AWS objects (@link com.amazonaws.services.simpledb.model.ReplaceableAttribute).
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
public class SimpleDBNativeItem {

    private Map<String, String> data = Collections.synchronizedMap(new HashMap<String, String>());

    public SimpleDBNativeItem() {}

    public SimpleDBNativeItem(Item item) {
        //populate map with the item attributes. //todo - handle multi-value attributes/long string etc
        List<Attribute> attributes = item.getAttributes();
        for (Attribute attribute : attributes) {
            put(attribute.getName(), attribute.getValue());
        }
    }

    public void put(String key, String value) {
        data.put(key, value);
    }

    public String get(String key) {
        return data.get(key);
    }

    public ReplaceableItem createReplaceableItem() {
        ReplaceableItem replaceableItem = new ReplaceableItem();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            //exclude id property because that will be specified as the item name
            String key = entry.getKey();
            if (!"id".equals(key)) {
                String value = entry.getValue();
                replaceableItem.withAttributes(new ReplaceableAttribute(key, value, true));
            }
        }
        return replaceableItem;
    }

    @Override
    public String toString() {
        return "SimpleDBNativeItem{data=" + data + '}';
    }
}
