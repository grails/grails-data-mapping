package org.grails.datastore.mapping.appengine;

import com.google.appengine.api.datastore.Transaction;

/**
 * @author Guillaume Laforge
 */
public class AppEngineTransaction implements org.grails.datastore.mapping.transactions.Transaction<Transaction> {
    private com.google.appengine.api.datastore.Transaction transaction;

    public AppEngineTransaction(com.google.appengine.api.datastore.Transaction transaction) {
        this.transaction = transaction;
    }

    public void commit() {
        transaction.commit();
    }

    public void rollback() {
        transaction.rollback();        
    }

    public com.google.appengine.api.datastore.Transaction getNativeTransaction() {
        return transaction;
    }

    public boolean isActive() {
        return transaction.isActive();
    }

    public void setTimeout(int timeout) {
        throw new UnsupportedOperationException("Transaction timeouts not supported on AppEngine");
    }
}
