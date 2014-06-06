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
    private ThreadLocal<Connection> connectionThreadLocal = new ThreadLocal<>();
//    private ThreadLocal<Stack<Connection>> connectionStackThreadLocal = new ThreadLocal<Stack<Connection>>() {
//        @Override
//        protected Stack<Connection> initialValue() {
//            return new Stack<Connection>();
//        }
//    };

    public JdbcCypherEngine(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public CypherResult execute(String cypher, List params) {
        try (Connection connection = dataSource.getConnection()) {
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
        try (Connection connection = dataSource.getConnection()) {
            logCypher(cypher, null);
            ResultSet rs = connection.createStatement().executeQuery(cypher);
            return new JdbcCypherResult(rs);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void beginTx() {
        try {
            log.info("beginTx");
            connectionThreadLocal.set(dataSource.getConnection());
//            Stack<Connection> stack = connectionStackThreadLocal.get();
//            stack.push(dataSource.getConnection());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void commit() {
        try {
            log.info("commit");
            Connection connection = connectionThreadLocal.get();
            connection.commit();
            connection.close();
            connectionThreadLocal.remove();
//            Stack<Connection> stack = connectionStackThreadLocal.get();
//            Connection connection = stack.pop();
//            connection.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void rollback() {
        try {
            log.info("rollback");
            connectionThreadLocal.get().rollback();
            connectionThreadLocal.get().close();
            connectionThreadLocal.remove();
//            Stack<Connection> stack = connectionStackThreadLocal.get();
//            Connection connection = stack.pop();
//            connection.rollback();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
