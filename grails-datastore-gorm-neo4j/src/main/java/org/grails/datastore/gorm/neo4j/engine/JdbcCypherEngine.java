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

import org.grails.datastore.gorm.neo4j.Neo4jUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.sql.DataSource;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.transaction.TransactionDefinition.*;

/**
 * CypherEngine implementation backed by a Neo4j JDBC datasource
 * @author Stefan Armbruster <stefan@armbruster-it.de>
 */
public class JdbcCypherEngine implements CypherEngine {

    private static Logger log = LoggerFactory.getLogger(JdbcCypherEngine.class);
    private final DataSource dataSource;
    private final GraphDatabaseService graphDatabaseService;
    final private ThreadLocal<Connection> connectionThreadLocal = new ThreadLocal<Connection>();
    final private ThreadLocal<AtomicInteger> transactionNestingDepth = new ThreadLocal<AtomicInteger>() {
        @Override
        protected AtomicInteger initialValue() {
            return new AtomicInteger();
        }
    };
    final private ThreadLocal<Map<Integer, Transaction>> suspendedTransactionsByDepthThreadLocal = new ThreadLocal<Map<Integer, Transaction>>() {
        @Override
        protected Map<Integer, Transaction> initialValue() {
            return new HashMap<Integer, Transaction>();
        }
    };
    final private ThreadLocal<Boolean> doRollback = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

    public JdbcCypherEngine(DataSource dataSource, GraphDatabaseService graphDatabaseService) {
        this.dataSource = dataSource;
        this.graphDatabaseService = graphDatabaseService;
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
            checkNestingDepthOnExecute();
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

    protected void checkNestingDepthOnExecute() {
        int depth = transactionNestingDepth.get().get();
        if (depth == 0 ) {
            beginTx();
            log.warn("execute with nesting depth 0, should only happen in rare cases (e.g. after session.disconnect()");
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
            checkNestingDepthOnExecute();
            Connection connection = getOrInitConnectionThreadLocal();
            logCypher(cypher, null);
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(cypher);
            return new JdbcCypherResult(rs);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void beginTx() {
        beginTx(new DefaultTransactionDefinition());
    }

    @Override
    public void beginTx(TransactionDefinition transactionDefinition) {
        switch (transactionDefinition.getPropagationBehavior()) {
            case PROPAGATION_REQUIRED:
                int depth = transactionNestingDepth.get().getAndIncrement();
                if (depth == 0) {
                    getOrInitConnectionThreadLocal();
                    doRollback.set(Boolean.FALSE);
                }
                Neo4jUtils.logWithCause(log, "beginTx", depth);
                break;
            case PROPAGATION_REQUIRES_NEW:
                depth = transactionNestingDepth.get().getAndIncrement();
                if (depth == 0) {
                    getOrInitConnectionThreadLocal();
                    doRollback.set(Boolean.FALSE);
                }
                if (graphDatabaseService != null) {
                    TransactionManager transactionManager = ((GraphDatabaseAPI) graphDatabaseService).getDependencyResolver().resolveDependency(TransactionManager.class);
                    try {
                        Transaction tx = transactionManager.suspend();
                        if (tx!=null) {
                            suspendedTransactionsByDepthThreadLocal.get().put(depth, tx);
                        }
                    } catch (SystemException e) {
                        throw new RuntimeException(e);
                    }
                }
                Neo4jUtils.logWithCause(log, "beginTx propagation NEW", depth);
                break;
            case PROPAGATION_MANDATORY:
            case PROPAGATION_NESTED:
            case PROPAGATION_NEVER:
            case PROPAGATION_NOT_SUPPORTED:
            case PROPAGATION_SUPPORTS:
            default:
                throw new IllegalStateException("neo4j plugin does not yet know how to handle propagation " + transactionDefinition.getPropagationBehavior());
        }
    }

    @Override
    public void commit() {
//        log.info("commit");
        int depth = transactionNestingDepth.get().decrementAndGet();

        if (depth>=0) {
            String amend = doRollback.get() ? " <fake, doRollback>" : "";
            Neo4jUtils.logWithCause(log, "commit" + amend, depth);
            if (depth == 0) {
                finishTransactionWithCommitOrRollback();
            }
            eventuallyResumeTransaction(depth);
        } else {
            transactionNestingDepth.get().incrementAndGet();
        }

    }

    private void eventuallyResumeTransaction(int depth) {
        if (graphDatabaseService!=null) {
            Transaction tx = suspendedTransactionsByDepthThreadLocal.get().remove(depth);
            if (tx!=null) {
                TransactionManager transactionManager = ((GraphDatabaseAPI)graphDatabaseService).getDependencyResolver().resolveDependency(TransactionManager.class);
                try {
                    transactionManager.resume(tx);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }

        }
    }

    /**
     * depending on {@link #doRollback} we either do a commit or a rollback
     */
    protected void finishTransactionWithCommitOrRollback() {
        // precondition
        int depth = transactionNestingDepth.get().get();
        if (depth!=0) {
            throw new IllegalStateException("we are still inside a nested transaction");
        }

        // action
        try {
            Connection connection = connectionThreadLocal.get();
            if (doRollback.get()) {
                connection.rollback();
            } else {
                connection.commit();
            }
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            connectionThreadLocal.remove();
        }
    }

    @Override
    public void rollback() {
//        log.info("rollback");
        int depth = transactionNestingDepth.get().decrementAndGet();
        Neo4jUtils.logWithCause(log, "rollback", depth);
        doRollback.set(Boolean.TRUE);
        if (depth == 0) {
            finishTransactionWithCommitOrRollback();
        }
        eventuallyResumeTransaction(depth);
    }

}
