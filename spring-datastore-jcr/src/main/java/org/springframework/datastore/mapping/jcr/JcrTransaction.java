package org.springframework.datastore.mapping.jcr;

import org.springframework.datastore.mapping.transactions.Transaction;
import org.springframework.extensions.jcr.JcrSessionFactory;
import org.springframework.extensions.jcr.jackrabbit.support.UserTxSessionHolder;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.UnexpectedRollbackException;

import javax.jcr.Session;


/**
 * @author Erawat Chamanont
 * @since 1.0
 */
public class JcrTransaction implements Transaction<UserTxSessionHolder> {
    private UserTxSessionHolder transaction;
    private boolean rollbackCalled;
    private boolean commitCalled;    

    public JcrTransaction(JcrSessionFactory sessionFactory) {
        try {
            this.transaction = new UserTxSessionHolder(sessionFactory.getSession());
            transaction.getTransaction().begin();
         } catch (Exception e) {
            throw new TransactionSystemException("Exception occurred beginning JackRabbit transaction: " + e.getMessage());
        }
    }
    public void commit() {
        if (rollbackCalled) {
            throw new IllegalTransactionStateException("Cannot call commit after rollback. Start another transaction first!");
        }
        try {
            transaction.getTransaction().commit();
            commitCalled = true;
        } catch (Exception e) {
            throw new TransactionSystemException("Exception occurred committing back JackRabbit transaction: " + e.getMessage());
        }
    }
    public boolean isActive() {
        return !commitCalled && !rollbackCalled;
    }

    public void setTimeout(int timeout) {
        try {
            transaction.getTransaction().setTransactionTimeout(timeout);
        } catch (Exception e) {
            throw new TransactionSystemException("Exception occurred setting timeout JackRabbit transaction: " + e.getMessage());
        }
    }

    public void rollback() {
        if (rollbackCalled) {
            throw new UnexpectedRollbackException("Cannot rollback JackRabbit transaction. Transaction already rolled back!");
        }
        try {
            transaction.getTransaction().rollback();
            rollbackCalled = true;
        } catch (Exception e) {
            throw new TransactionSystemException("Exception occurred rolling back JackRabbit transaction: " + e.getMessage());
        }
    }

    public UserTxSessionHolder getNativeTransaction() {
        return this.transaction;
    }


}