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
package org.grails.datastore.mapping.dynamodb;

import org.grails.datastore.mapping.cache.TPCacheAdapterRepository;
import org.grails.datastore.mapping.core.AbstractSession;
import org.grails.datastore.mapping.engine.Persister;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.dynamodb.engine.DynamoDBEntityPersister;
import org.grails.datastore.mapping.dynamodb.query.DynamoDBQuery;
import org.grails.datastore.mapping.dynamodb.util.DynamoDBTemplate;
import org.grails.datastore.mapping.transactions.SessionOnlyTransaction;
import org.grails.datastore.mapping.transactions.Transaction;
import org.springframework.context.ApplicationEventPublisher;

/**
 * A {@link org.grails.datastore.mapping.core.Session} implementation
 * for the AWS DynamoDB store.
 *
 * @author Roman Stepanenko based on Graeme Rocher code for MongoDb and Redis
 * @since 0.1
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class DynamoDBSession extends AbstractSession {

    DynamoDBDatastore dynamoDBDatastore;

    public DynamoDBSession(DynamoDBDatastore datastore, MappingContext mappingContext, ApplicationEventPublisher publisher, TPCacheAdapterRepository cacheAdapterRepository) {
        super(datastore, mappingContext, publisher, cacheAdapterRepository);
        this.dynamoDBDatastore = datastore;
    }

    @Override
    public DynamoDBQuery createQuery(Class type) {
        return (DynamoDBQuery) super.createQuery(type);
    }

/*    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void flushPendingInserts(Map<PersistentEntity, Collection<PendingInsert>> inserts) {
        //todo - optimize multiple inserts using batch put (make the number of threshold objects configurable)
        for (final PersistentEntity entity : inserts.keySet()) {
            final DynamoDBTemplate template = getDynamoDBTemplate(entity.isRoot() ? entity : entity.getRootEntity());

            throw new RuntimeException("not implemented yet");
//            template.persist(null); //todo - :)
        }
    }
    */

    public Object getNativeInterface() {
        return null; //todo
    }

    @Override
    protected Persister createPersister(Class cls, MappingContext mappingContext) {
        final PersistentEntity entity = mappingContext.getPersistentEntity(cls.getName());
        return entity == null ? null : new DynamoDBEntityPersister(mappingContext, entity, this, publisher, cacheAdapterRepository);
    }

    @Override
    protected Transaction beginTransactionInternal() {
        return new SessionOnlyTransaction(null, this);
    }

    public DynamoDBTemplate getDynamoDBTemplate() {
        return dynamoDBDatastore.getDynamoDBTemplate();
    }
}
