package org.grails.datastore.gorm.neo4j

import org.springframework.datastore.mapping.transactions.Transaction
import sun.reflect.generics.reflectiveObjects.NotImplementedException

/**
 * Created by IntelliJ IDEA.
 * User: stefan
 * Date: 08.05.11
 * Time: 19:30
 * To change this template use File | Settings | File Templates.
 */
class Neo4jTransaction implements Transaction {

    org.neo4j.graphdb.Transaction nativeTransaction
    boolean active = true

    public Neo4jTransaction(org.neo4j.graphdb.Transaction nativeTransaction) {
        this.nativeTransaction = nativeTransaction
    }

    @Override
    void commit() {
        nativeTransaction.success()
        active = false
    }

    @Override
    void rollback() {
        nativeTransaction.failure()
        active = false
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
