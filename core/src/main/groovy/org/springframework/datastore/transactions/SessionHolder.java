/* Copyright (C) 2010 SpringSource
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.datastore.transactions;

import org.springframework.datastore.core.Session;
import org.springframework.transaction.support.ResourceHolderSupport;

/**
 * Holds a reference to the session
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class SessionHolder extends ResourceHolderSupport {


    private Session session;
    private Transaction transaction;

    public SessionHolder(Session session) {
        this.session = session;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public Session getSession() {
        return session;
    }

    public boolean isEmpty() {
        return session == null;
    }

    public boolean doesNotHoldNonDefaultSession() {
        return isEmpty();
    }

    public void addSession(Session session) {
        this.session = session;
    }

    public boolean containsSession(Session session) {
        return this.session == session;
    }

}
