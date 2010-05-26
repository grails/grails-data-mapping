package org.grails.inconsequential.appengine;

/**
 * @author Guillaume Laforge
 */
public class AppEngineTransaction implements org.grails.inconsequential.tx.Transaction<com.google.appengine.api.datastore.Transaction> {
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
}
