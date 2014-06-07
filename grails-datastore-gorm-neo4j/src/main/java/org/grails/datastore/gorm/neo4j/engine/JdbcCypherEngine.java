package org.grails.datastore.gorm.neo4j.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
            PreparedStatement ps = connection.prepareStatement(cypher);
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
            ResultSet rs = connection.createStatement().executeQuery(cypher);
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
            connectionThreadLocal.remove();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void rollback() {
        try {
            log.info("rollback");
            Connection connection = connectionThreadLocal.get();
            connection.rollback();
            connection.close();
            connectionThreadLocal.remove();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
