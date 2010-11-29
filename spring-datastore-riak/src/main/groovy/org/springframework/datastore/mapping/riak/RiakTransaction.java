package org.springframework.datastore.mapping.riak;

import org.springframework.datastore.mapping.transactions.Transaction;
import org.springframework.data.riak.core.RiakTemplate;
import org.springframework.transaction.IllegalTransactionStateException;

/**
 * @author J. Brisbin <jon@jbrisbin.com>
 */
public class RiakTransaction implements Transaction<RiakTemplate> {

  private RiakTemplate riakTemplate;
  private boolean committed = false;
  private boolean rolledBack = false;

  public RiakTransaction(RiakTemplate riakTemplate) {
    this.riakTemplate = riakTemplate;
  }

  public void commit() {
    if (rolledBack) {
      throw new IllegalTransactionStateException("This transaction has already been rolled back!");
    }
    committed = true;
  }

  public void rollback() {
    if (committed) {
      throw new IllegalTransactionStateException("This transaction has already been committed!");
    }
    rolledBack = true;
  }

  public RiakTemplate getNativeTransaction() {
    return riakTemplate;
  }

  public boolean isActive() {
    return !committed && !rolledBack;
  }

  public void setTimeout(int timeout) {
    throw new UnsupportedOperationException("Transaction timeouts do not apply to the Riak support.");
  }

}
