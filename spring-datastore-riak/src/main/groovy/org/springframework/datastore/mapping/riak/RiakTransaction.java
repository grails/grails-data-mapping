/*
 * Copyright (c) 2010 by J. Brisbin <jon@jbrisbin.com>
 *     Portions (c) 2010 by NPC International, Inc. or the
 *     original author(s).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.datastore.mapping.riak;

import org.springframework.data.keyvalue.riak.core.RiakTemplate;
import org.springframework.datastore.mapping.transactions.Transaction;
import org.springframework.transaction.IllegalTransactionStateException;

/**
 * A {@link org.springframework.datastore.mapping.transactions.Transaction} implemenatation for
 * Riak.
 * <p/>
 * Note that this implementation doesn't do any real transaction processing because Riak doesn't
 * have native transaction support.
 *
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
      throw new IllegalTransactionStateException(
          "This transaction has already been rolled back!");
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
    throw new UnsupportedOperationException(
        "Transaction timeouts do not apply to the Riak support.");
  }

}
