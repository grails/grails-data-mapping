package org.springframework.datastore.tx;

/**
 * Class giving the ability to start, commit and rollback a transaction.
 *
 * @author Guillaume Laforge
 */
public interface Transaction<T> {

    /**
     * Commit the transaction.
     */
    void commit();

    /**
     * Rollback the transaction.
     */
    void rollback();

    /**
     * @return the native transaction object.
     */
    T getNativeTransaction();
}
