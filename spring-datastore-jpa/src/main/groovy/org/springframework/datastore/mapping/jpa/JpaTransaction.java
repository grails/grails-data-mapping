package org.springframework.datastore.mapping.jpa;

import org.springframework.datastore.mapping.transactions.Transaction;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionStatus;

public class JpaTransaction implements Transaction {

    private TransactionStatus transaction;
    private JpaTransactionManager transactionManager;

    public JpaTransaction(JpaTransactionManager transactionManager, TransactionStatus transaction) {
        this.transaction = transaction;
        this.transactionManager = transactionManager;
    }

    public void commit() {
        if (isValidTransactionPresent()) {
            transactionManager.commit(transaction);
        }
    }

    boolean isValidTransactionPresent() {
        return transaction != null && !transaction.isCompleted();
    }

    public void rollback() {
        if (isValidTransactionPresent()) {
            transactionManager.rollback(transaction);
        }
    }

    public Object getNativeTransaction() {
        return transaction;
    }

    public boolean isActive() {
        return isValidTransactionPresent();
    }

    public void setTimeout(int timeout) {
        transactionManager.setDefaultTimeout(timeout);
    }
}
