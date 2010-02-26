package org.grails.inconsequential;

/**
 * Class giving the ability to start, commit and rollback a transaction.
 *
 * @author Guillaume Laforge
 */
public interface Transaction {

    /**
     * Begin a new transaction.
     */
    void begin();

    /**
     * Commit the transaction.
     */
    void commit();

    /**
     * Rollback the transaction.
     */
    void rollback();
}
