/* Copyright (C) 2013 original authors
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
package org.grails.datastore.mapping.rest.client

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.cache.TPCacheAdapterRepository
import org.grails.datastore.mapping.core.AbstractSession
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.core.impl.PendingInsert
import org.grails.datastore.mapping.core.impl.PendingUpdate
import org.grails.datastore.mapping.engine.Persister
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.rest.client.engine.RestClientEntityPersister
import org.grails.datastore.mapping.transactions.SessionOnlyTransaction
import org.grails.datastore.mapping.transactions.Transaction
import org.springframework.context.ApplicationEventPublisher

/**
 * Session implementation for REST clients
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
class RestClientSession extends AbstractSession{

    static final String ATTRIBUTE_URL = "url"
    static final String ATTRIBUTE_REQUEST_CUSTOMIZER = "request-customizer"

    RestClientSession(Datastore datastore, MappingContext mappingContext, ApplicationEventPublisher publisher) {
        super(datastore, mappingContext, publisher)
    }

    RestClientSession(Datastore datastore, MappingContext mappingContext, ApplicationEventPublisher publisher, boolean stateless) {
        super(datastore, mappingContext, publisher, stateless)
    }

    RestClientSession(Datastore datastore, MappingContext mappingContext, ApplicationEventPublisher publisher, TPCacheAdapterRepository cacheAdapterRepository) {
        super(datastore, mappingContext, publisher, cacheAdapterRepository)
    }

    RestClientSession(Datastore datastore, MappingContext mappingContext, ApplicationEventPublisher publisher, TPCacheAdapterRepository cacheAdapterRepository, boolean stateless) {
        super(datastore, mappingContext, publisher, cacheAdapterRepository, stateless)
    }

    @Override
    Serializable insert(Object o) {
        if(o) {
            RestClientEntityPersister persister = (RestClientEntityPersister)getPersister(o)
            if(persister != null) {
                persister.insert(o)
            }
        }
    }

    @Override
    Object getNativeInterface() {
        // no native interface, just return this
        return this
    }

    @Override
    protected Persister createPersister(Class cls, MappingContext mappingContext) {
        final entity = mappingContext.getPersistentEntity(cls.name)
        if(entity != null) {
            return new RestClientEntityPersister(mappingContext, entity, this, publisher)
        }
        else {
            throw new IllegalArgumentException("Class [$cls.name] is not a persistent entity")
        }
    }

    @Override
    protected Transaction beginTransactionInternal() {
        return new SessionOnlyTransaction(this, this)
    }
}
