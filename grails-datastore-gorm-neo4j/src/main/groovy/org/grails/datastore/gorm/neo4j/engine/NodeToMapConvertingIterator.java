package org.grails.datastore.gorm.neo4j.engine;

import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Node;
import java.util.HashMap;
import java.util.Map;

/**
 * wrap an standard iterator from a ExecutionResult to return maps instead of Node
 */
public class NodeToMapConvertingIterator implements ResourceIterator<Map<String, Object>> {

    private ResourceIterator<Map<String, Object>> delegatingIterator;

    NodeToMapConvertingIterator(ResourceIterator<Map<String, Object>> delegatingIterator) {
        this.delegatingIterator = delegatingIterator;
    }

    @Override
    public void close() {
        delegatingIterator.close();
    }

    @Override
    public boolean hasNext() {
        return delegatingIterator.hasNext();
    }

    @Override
    public Map<String, Object> next() {
        Map<String, Object> row = (Map<String, Object>)delegatingIterator.next();
        Map<String, Object> result = new HashMap<String, Object>();
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            Object value;
            if (entry.getValue() instanceof org.neo4j.graphdb.Node) {
                Map<String,Object> map = new HashMap<String, Object>();
                Node node = (Node) entry.getValue();
                for (String key : node.getPropertyKeys()) {
                    map.put(key, node.getProperty(key));
                }
                value = map;
            } else {
                value = entry.getValue();
            }
            result.put(entry.getKey(), value);
        }
        return result;
    }

    @Override
    public void remove() {
        delegatingIterator.remove();
    }

}
