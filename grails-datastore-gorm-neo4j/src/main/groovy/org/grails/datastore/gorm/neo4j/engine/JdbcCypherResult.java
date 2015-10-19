package org.grails.datastore.gorm.neo4j.engine;

import org.neo4j.graphdb.ResourceIterator;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by stefan on 05.05.14.
 */
public class JdbcCypherResult implements CypherResult {

    private final ResultSet resultSet;

    public JdbcCypherResult(ResultSet resultSet) {
        this.resultSet = resultSet;
    }

    @Override
    public List<String> getColumns() {
        try {
            ResultSetMetaData metaData = resultSet.getMetaData();
            int count = metaData.getColumnCount();
            List<String> result = new ArrayList<String>(count);
            for (int i = 1; i <= count; i++) {
                result.add(metaData.getColumnName(i));
            }
            return result;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ResourceIterator<Map<String, Object>> iterator() {
        return new ResultSetIterator(resultSet);
    }

}
