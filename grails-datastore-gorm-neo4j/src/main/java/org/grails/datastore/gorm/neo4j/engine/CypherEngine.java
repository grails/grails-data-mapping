package org.grails.datastore.gorm.neo4j.engine;

import org.springframework.transaction.TransactionDefinition;

import java.util.List;
import java.util.Map;

/**
 * abstraction to use cypher via embedded and remote the same way
 *
 * @author Stefan
 *
 */
public interface CypherEngine {

    /**
     * only positional parameters are valid in cypher over jdbc
     * The argument list references them by position starting with 1,
     * just like jdbc's preparedstatements do
     * @param cypher cypher string containing positional parameters e.g.<code>MATCH (n:Person {name:{1}}) RETURN n</code>
     * @param params parameters for the above query
     * @return
     */
    CypherResult execute(String cypher, List params);

    /**
     * Execute a cypher query
     *
     * @param cypher The cypher string
     * @return A cypher result
     */
    CypherResult execute(String cypher);

    /**
     * Start a transaction
     */
    void beginTx();

    /**
     * Strat a transaction for the given definition
     *
     * @param transactionDefinition The definition
     */
    void beginTx(TransactionDefinition transactionDefinition);

    /**
     * Commits the transaction
     */
    void commit();

    /**
     * Rollback the transaction
     */
    void rollback();
}