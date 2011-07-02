package org.springframework.datastore.mapping.simpledb.engine;

import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import com.amazonaws.services.simpledb.model.ReplaceableItem;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Logical representation of how information is loaded from and sent to AWS.
 * <p/>
 * It stores all data in an internal Map and then creates appropriate AWS objects (@link com.amazonaws.services.simpledb.model.ReplaceableAttribute).
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
public class NativeSimpleDBItem {
    public NativeSimpleDBItem() {
    }

    public NativeSimpleDBItem(Item item) {
        //populate map with the item attributes. //todo - handle multi-value attributes/long string etc
        List<Attribute> attributes = item.getAttributes();
        for (Attribute attribute : attributes) {
            put(attribute.getName(), attribute.getValue());
        }
    }

    public void put(String key, String value) {
        if ( value == null ) {
            data.remove(key); //concurrent hash map does not allow null values
        } else {
            data.put(key, value);
        }
    }

    public String get(String key) {
        return data.get(key);
    }

    public ReplaceableItem createReplaceableItem() {
        ReplaceableItem replaceableItem = new ReplaceableItem();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            //exclude id property because that will be specified as the item name
            String key = entry.getKey();
            if ( !"id".equals(key) ) {
                String value = entry.getValue();
                replaceableItem.withAttributes(new ReplaceableAttribute(key, value, true));
            }
        }
        return replaceableItem;
    }

    @Override
    public String toString() {
        return "NativeSimpleDBItem{" +
                "data=" + data +
                '}';
    }

    private Map<String, String> data = new ConcurrentHashMap<String, String>(); //todo - not sure about concurrency requirements - can it be simplified to use HashMap?
}
