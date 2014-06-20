package org.grails.datastore.gorm.neo4j.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.List;

/**
 * CypherEngine implementation backed by {@link org.neo4j.cypher.javacompat.ExecutionEngine}
 */
public class JdbcCypherEngine implements CypherEngine {

    private static Logger log = LoggerFactory.getLogger(JdbcCypherEngine.class);
    private final DataSource dataSource;
    private ThreadLocal<Connection> connectionThreadLocal = new ThreadLocal<Connection>() {
        @Override
        protected Connection initialValue() {
            try {
                return dataSource.getConnection();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    };

    public JdbcCypherEngine(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public CypherResult execute(String cypher, List params) {
        try {
            Connection connection = connectionThreadLocal.get();
            logCypher(cypher, params);
            PreparedStatement ps;
            try {
                ps = connection.prepareStatement(cypher);
            } catch (SQLException e) { // TODO: hackish wordaround since connection is closed when view is rendered
                connectionThreadLocal.remove();
                connection = connectionThreadLocal.get();
                ps = connection.prepareStatement(cypher);
            }
            for (int i=0; i<params.size(); i++) {
                ps.setObject(i+1, params.get(i));
            }
            ResultSet rs = ps.executeQuery();
            return new JdbcCypherResult(rs);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void logCypher(String cypher, Object params) {
        log.info("running cypher {}", cypher);
        if (params!=null) {
            log.info("   with params {}", params);
        }
    }

    @Override
    public CypherResult execute(String cypher) {
        try {
            Connection connection = connectionThreadLocal.get();
            logCypher(cypher, null);
            Statement statement;
            try {
                statement = connection.createStatement();
            } catch (SQLException e) { // TODO: hackish wordaround since connection is closed when view is rendered
                connectionThreadLocal.remove();
                connection = connectionThreadLocal.get();
                statement = connection.createStatement();
            }

            ResultSet rs = statement.executeQuery(cypher);
            return new JdbcCypherResult(rs);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * intentionally a noop here since execute open a tx implicitly
     */
    @Override
    public void beginTx() {
        log.info("beginTx");
    }

    @Override
    public void commit() {
        try {
            log.info("commit");
            Connection connection = connectionThreadLocal.get();
            connection.commit();
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            connectionThreadLocal.remove();
        }
    }

    @Override
    public void rollback() {
        try {
            log.info("rollback");
            Connection connection = connectionThreadLocal.get();
            connection.rollback();
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            connectionThreadLocal.remove();
        }
    }

}
