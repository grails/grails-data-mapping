package org.grails.datastore.gorm.neo4j

import org.springframework.datastore.mapping.transactions.Transaction
import sun.reflect.generics.reflectiveObjects.NotImplementedException
import org.slf4j.LoggerFactory
import org.slf4j.Logger
import org.neo4j.graphdb.GraphDatabaseService

/**
 * wrapping a Neo4j {@link org.neo4j.graphdb.Transaction} into a Spring data mapping {@link Transaction}
 * @author Stefan Armbruster <stefan@armbruster-it.de>
 */
class Neo4jTransaction implements Transaction {

    private static final Logger log = LoggerFactory.getLogger(Neo4jTransaction.class);

    GraphDatabaseService graphDatabaseService
    org.neo4j.graphdb.Transaction nativeTransaction
    boolean active = true

    public Neo4jTransaction(GraphDatabaseService graphDatabaseService) {
        this.graphDatabaseService = graphDatabaseService
        nativeTransaction = graphDatabaseService.beginTx()
        log.debug "new: $nativeTransaction"
    }

    @Override
    void commit() {
        log.debug "commit $nativeTransaction"
        nativeTransaction.success()
        nativeTransaction.finish()
        //nativeTransaction = graphDatabaseService.beginTx()
//        active = false
    }

    @Override
    void rollback() {
        log.debug "rollback $nativeTransaction"
        nativeTransaction.failure()
        nativeTransaction.finish()
        //nativeTransaction = graphDatabaseService.beginTx()
//        active = false
    }

    @Override
    Object getNativeTransaction() {
        nativeTransaction
    }

    @Override
    boolean isActive() {
        active
    }

    @Override
    void setTimeout(int timeout) {
        throw new NotImplementedException()
    }
}
