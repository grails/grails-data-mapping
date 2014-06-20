package org.grails.datastore.gorm.neo4j.engine;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by stefan on 02.06.14.
 */
public class ResultSetIterator implements ResourceIterator<Map<String, Object>> {

    private final ResultSet resultSet;

    public ResultSetIterator(ResultSet resultSet) {
        this.resultSet = resultSet;
    }

    @Override
    public void close() {
        try {
            resultSet.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean hasNext() {
        try {
            return !resultSet.isLast();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Object> next() {
        try {
            resultSet.next();
            ResultSetMetaData metaData = resultSet.getMetaData();
            int cols = metaData.getColumnCount();
            Map<String, Object> map = new HashMap<String, Object>(cols);
            for (int i=1; i<=cols; i++) {
                Object value = resultSet.getObject(i);
                map.put(metaData.getColumnName(i), convertNodeToMap(value));
            }
            return map;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * make sure a Neo4j node instance is converted to a map
     * @param value
     * @return
     */
    private Object convertNodeToMap(Object value) {
        if (value instanceof Node) {
            Map<String,Object> map = new HashMap<String, Object>();
            Node node = (Node) value;
            for (String key : node.getPropertyKeys()) {
                map.put(key, node.getProperty(key));
            }
            return map;
        } else {
            return value;
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
