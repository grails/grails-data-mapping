package org.grails.datastore.gorm.neo4j.engine;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * CypherEngine implementation backed by {@link ExecutionEngine}
 * @deprecated due to usage of {@link org.grails.datastore.gorm.neo4j.engine.JdbcCypherEngine}
 */
@Deprecated
public class EmbeddedCypherEngine implements CypherEngine {

    private static Logger log = LoggerFactory.getLogger(EmbeddedCypherEngine.class);
    private ExecutionEngine executionEngine;
    private GraphDatabaseService graphDatabaseService;

    private static ThreadLocal<Stack<Transaction>> transactionStackThreadLocal = new ThreadLocal<Stack<Transaction>>() {
        @Override
        protected Stack<Transaction> initialValue() {
            return new Stack<Transaction>();
        }
    };

    public EmbeddedCypherEngine(GraphDatabaseService graphDatabaseService) {
        this(graphDatabaseService, new ExecutionEngine(graphDatabaseService));
    }

    public EmbeddedCypherEngine(GraphDatabaseService graphDatabaseService, ExecutionEngine executionEngine) {
        this.graphDatabaseService = graphDatabaseService;
        this.executionEngine = executionEngine;
    }

    @Override
    public CypherResult execute(String cypher, List params) {
        Map paramsMap = null;

        if (params!=null) {
            paramsMap = new HashMap();
            for (int i = 0; i < params.size(); i++) {
                paramsMap.put(Integer.toString(i), params.get(i));
            }
        }
        return new EmbeddedCypherResult(paramsMap == null ?
                executionEngine.execute(cypher) :
                executionEngine.execute(cypher, paramsMap));
    }

    @Override
    public CypherResult execute(String cypher) {
        return execute(cypher, null);
    }

    @Override
    public void beginTx() {
        Stack transactionStack = transactionStackThreadLocal.get();
        transactionStack.push(graphDatabaseService.beginTx());

        log.info("beginTx: " + transactionStack);
    }

    @Override
    public void commit() {
        try {
            Stack<Transaction> transactionStack = transactionStackThreadLocal.get();
            log.info("commit: " + transactionStack);
            if (!transactionStack.isEmpty()) { // in case session.disconnect() get manually called
                Transaction tx = transactionStack.pop();
                log.info("commit after: " + transactionStack);
                tx.success();
                tx.close();
            }
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    public void rollback() {
        Stack<Transaction> transactionStack = transactionStackThreadLocal.get();
        log.info("rollback: " + transactionStack);
        Transaction tx = transactionStack.pop();
        log.info("rollback: " + transactionStack);
        tx.failure();
        tx.close();
    }

}
