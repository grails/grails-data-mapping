package org.grails.datastore.gorm.neo4j

import groovy.transform.CompileStatic
import org.springframework.transaction.TransactionException
import org.springframework.transaction.support.DefaultTransactionStatus

import org.grails.datastore.mapping.transactions.DatastoreTransactionManager
import org.grails.datastore.mapping.transactions.TransactionObject

@CompileStatic
public class Neo4jDatastoreTransactionManager extends DatastoreTransactionManager {

    @Override
    protected boolean isExistingTransaction(Object transaction) throws TransactionException {
        TransactionObject txObject = (TransactionObject) transaction
        return txObject != null && txObject.getTransaction() != null && txObject.getTransaction().isActive()
    }

    @Override
    protected void doSetRollbackOnly(DefaultTransactionStatus status) {
        TransactionObject txObject = (TransactionObject) status.getTransaction();
        if (status.isDebug()) {
            logger.debug("Setting Neo4j transaction on session [" + txObject.getSessionHolder().getSession() + "] rollback-only")
        }
    }

}
