/* Copyright (C) 2010 SpringSource
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
package org.grails.datastore.gorm.neo4j.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.List;

/**
 * CypherEngine implementation backed by a Neo4j JDBC datasource
 * @author Stefan Armbruster <stefan@armbruster-it.de>
 */
public class JdbcCypherEngine implements CypherEngine {

    private static Logger log = LoggerFactory.getLogger(JdbcCypherEngine.class);
    private final DataSource dataSource;
    private ThreadLocal<Connection> connectionThreadLocal = new ThreadLocal<Connection>();

    public JdbcCypherEngine(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private Connection getOrInitConnectionThreadLocal() {
        Connection connection = connectionThreadLocal.get();
        if (connection==null) {
            try {
                connection = dataSource.getConnection();
                connectionThreadLocal.set(connection);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return connection;
    }

    @Override
    public CypherResult execute(String cypher, List params) {
        try {
            Connection connection = getOrInitConnectionThreadLocal();
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
            Connection connection = getOrInitConnectionThreadLocal();
            logCypher(cypher, null);
            Statement statement = connection.createStatement();
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
        if (connectionThreadLocal.get()==null) {
            log.info("beginTx");
            getOrInitConnectionThreadLocal();
        }
    }

    @Override
    public void commit() {
        Connection connection = connectionThreadLocal.get();
        if (connection != null) {
            try {
                log.info("commit");
                connection.commit();
                connection.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                connectionThreadLocal.remove();
            }
        }
    }

    @Override
    public void rollback() {
        Connection connection = connectionThreadLocal.get();
        if (connection!=null) {
            try {
                log.info("rollback");
                connection.rollback();
                connection.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                connectionThreadLocal.remove();
            }
        }
    }

}
