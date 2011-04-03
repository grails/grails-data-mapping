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
package org.springframework.datastore.mapping.gemfire;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.datastore.mapping.core.AbstractSession;
import org.springframework.datastore.mapping.engine.Persister;
import org.springframework.datastore.mapping.gemfire.engine.GemfireEntityPersister;
import org.springframework.datastore.mapping.model.MappingContext;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.transactions.Transaction;

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.CacheTransactionManager;

/**
 * Implementation of the {@link org.springframework.datastore.mapping.core.Session} interface
 * that interacts with a Gemfire cache implementation
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class GemfireSession extends AbstractSession<Cache> {
    public GemfireSession(GemfireDatastore datastore, MappingContext mappingContext,
               ApplicationEventPublisher publisher) {
        super(datastore, mappingContext, publisher);
    }

    @Override
    protected Persister createPersister(Class cls, MappingContext mappingContext) {
        final PersistentEntity entity = mappingContext.getPersistentEntity(cls.getName());
        return entity == null ? null : new GemfireEntityPersister(mappingContext, entity, this, publisher);
    }

    @Override
    protected Transaction<CacheTransactionManager> beginTransactionInternal() {
        GemfireDatastore datastore = (GemfireDatastore) getDatastore();
        final CacheTransactionManager tm = datastore.getGemfireCache().getCacheTransactionManager();
        tm.begin();
        return new GemfireTransaction(tm);
    }

    public boolean isConnected() {
        return true; // TODO: Support Client Caches, here we assume a peer cache
    }

    public Object getNativeInterface() {
        return ((GemfireDatastore)getDatastore()).getGemfireCache();
    }

    private class GemfireTransaction implements Transaction<CacheTransactionManager> {

        private CacheTransactionManager transactionManager;

        public GemfireTransaction(CacheTransactionManager transactionManager) {
            this.transactionManager = transactionManager;
        }

        public void commit() {
            transactionManager.commit();
        }

        public void rollback() {
            transactionManager.rollback();
        }

        public CacheTransactionManager getNativeTransaction() {
            return transactionManager;
        }

        public boolean isActive() {
            return transactionManager.exists();
        }

        public void setTimeout(int timeout) {
            // noop
        }
    }
}
