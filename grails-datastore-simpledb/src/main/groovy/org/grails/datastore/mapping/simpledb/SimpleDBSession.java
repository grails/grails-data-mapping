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
package org.grails.datastore.mapping.simpledb;

import org.springframework.context.ApplicationEventPublisher;
import org.grails.datastore.mapping.core.AbstractSession;
import org.grails.datastore.mapping.engine.Persister;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.simpledb.engine.SimpleDBEntityPersister;
import org.grails.datastore.mapping.simpledb.query.SimpleDBQuery;
import org.grails.datastore.mapping.simpledb.util.SimpleDBTemplate;
import org.grails.datastore.mapping.transactions.SessionOnlyTransaction;
import org.grails.datastore.mapping.transactions.Transaction;

/**
 * A {@link org.grails.datastore.mapping.core.Session} implementation
 * for the AWS SimpleDB store.
 *
 * @author Roman Stepanenko based on Graeme Rocher code for MongoDb and Redis
 * @since 0.1
 */
public class SimpleDBSession extends AbstractSession {

    SimpleDBDatastore simpleDBDatastore;

    public SimpleDBSession(SimpleDBDatastore datastore, MappingContext mappingContext, ApplicationEventPublisher publisher) {
        super(datastore, mappingContext, publisher);
        this.simpleDBDatastore = datastore;
    }

    @Override
    public SimpleDBQuery createQuery(@SuppressWarnings("rawtypes") Class type) {
        return (SimpleDBQuery) super.createQuery(type);
    }

/*    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void flushPendingInserts(Map<PersistentEntity, Collection<PendingInsert>> inserts) {
        //todo - optimize multiple inserts using batch put (make the number of threshold objects configurable)
        for (final PersistentEntity entity : inserts.keySet()) {
            final SimpleDBTemplate template = getSimpleDBTemplate(entity.isRoot() ? entity : entity.getRootEntity());

            throw new RuntimeException("not implemented yet");
//            template.persist(null); //todo - :)
        }
    }
    */

    public Object getNativeInterface() {
        return null; //todo
    }

    @Override
    protected Persister createPersister(@SuppressWarnings("rawtypes") Class cls, MappingContext mappingContext) {
        final PersistentEntity entity = mappingContext.getPersistentEntity(cls.getName());
        return entity == null ? null : new SimpleDBEntityPersister(mappingContext, entity, this, publisher);
    }

    @Override
    protected Transaction beginTransactionInternal() {
        return new SessionOnlyTransaction(null, this);
    }

    public SimpleDBTemplate getSimpleDBTemplate(PersistentEntity entity) {
        return simpleDBDatastore.getSimpleDBTemplate(entity);
    }
}
