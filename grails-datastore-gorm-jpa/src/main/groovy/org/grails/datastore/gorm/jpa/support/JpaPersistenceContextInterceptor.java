/* Copyright (C) 2011 SpringSource
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
package org.grails.datastore.gorm.jpa.support;

import org.grails.datastore.gorm.support.DatastorePersistenceContextInterceptor;
import org.springframework.datastore.mapping.core.DatastoreUtils;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.jpa.JpaDatastore;
import org.springframework.datastore.mapping.transactions.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Graeme Rocher
 * @since 1.0
 */
public class JpaPersistenceContextInterceptor extends DatastorePersistenceContextInterceptor {

    private JpaDatastore jpaDatastore;

    public JpaPersistenceContextInterceptor(JpaDatastore datastore) {
        super(datastore);
        this.jpaDatastore = datastore;
    }

    @Override
    protected Session getSession() {
        SessionHolder sessionHolder = (SessionHolder)TransactionSynchronizationManager.getResource(jpaDatastore);
        if (sessionHolder != null) {
            return sessionHolder.getSession();
        }

        return DatastoreUtils.bindSession(jpaDatastore.connect());
    }

    @Override
    public void flush() {
        // do nothing
    }
}
