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

package org.springframework.datastore.mapping.mongo;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.data.document.mongodb.DbCallback;
import org.springframework.data.document.mongodb.MongoTemplate;
import org.springframework.datastore.mapping.core.AbstractSession;
import org.springframework.datastore.mapping.core.impl.PendingInsert;
import org.springframework.datastore.mapping.core.impl.PendingOperation;
import org.springframework.datastore.mapping.document.config.DocumentMappingContext;
import org.springframework.datastore.mapping.engine.Persister;
import org.springframework.datastore.mapping.model.MappingContext;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.mongo.engine.MongoEntityPersister;
import org.springframework.datastore.mapping.mongo.query.MongoQuery;
import org.springframework.datastore.mapping.transactions.SessionOnlyTransaction;
import org.springframework.datastore.mapping.transactions.Transaction;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;

/**
 * A {@link org.springframework.datastore.mapping.core.Session} implementation for the Mongo document store.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class MongoSession extends AbstractSession<DB> {

    MongoDatastore mongoDatastore;
    private WriteConcern writeConcern = WriteConcern.NORMAL;

    public MongoSession(MongoDatastore datastore, MappingContext mappingContext, ApplicationEventPublisher publisher) {
        super(datastore, mappingContext, publisher);
        this.mongoDatastore = datastore;
        try {
            getNativeInterface().requestStart();
        }
        catch (IllegalStateException ignored) {
            // can't call authenticate() twice, and it's probably been called at startup
        }
    }

    @Override
    public MongoQuery createQuery(@SuppressWarnings("rawtypes") Class type) {
        return (MongoQuery) super.createQuery(type);
    }

    /**
     * Sets the WriteConcern to use for the session
     *
     * @param writeConcern The WriteConcern to use
     */
    public void setWriteConcern(WriteConcern writeConcern) {
        this.writeConcern = writeConcern;
    }

    /**
     * Obtains the WriteConcern to use for the session
     * @return the WriteConcern
     */
    public WriteConcern getWriteConcern() {
        return writeConcern;
    }

    /**
     * Execute a flush using the given WriteConcern
     *
     * @param writeConcern The WriteConcern to use
     */
    public void flush(@SuppressWarnings("hiding") WriteConcern writeConcern) {
        WriteConcern current = this.writeConcern;

        this.writeConcern = writeConcern;

        try {
            super.flush();
        }
        finally {
            this.writeConcern = current;
        }
    }

    @Override
    public void disconnect() {
        super.disconnect();
        getNativeInterface().requestDone();
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void flushPendingInserts(final Map<PersistentEntity, Collection<PendingInsert>> inserts) {
        // Optimizes saving multipe entities at once
        for (final PersistentEntity entity : inserts.keySet()) {
            final MongoTemplate template = getMongoTemplate(entity.isRoot() ? entity : entity.getRootEntity());
            template.execute(new DbCallback<Object>() {
                public Object doInDB(DB db) throws MongoException, DataAccessException {
                    final DBCollection collection = db.getCollection(template.getDefaultCollectionName());

                    final Collection<PendingInsert> pendingInserts = inserts.get(entity);
                    List<DBObject> dbObjects = new LinkedList<DBObject>();
                    List<PendingOperation> postOperations = new LinkedList<PendingOperation>();

                    for (PendingInsert pendingInsert : pendingInserts) {

                        final List<PendingOperation> preOperations = pendingInsert.getPreOperations();
                        for (PendingOperation preOperation : preOperations) {
                            preOperation.run();
                        }

                        dbObjects.add((DBObject) pendingInsert.getNativeEntry());
                        postOperations.addAll(pendingInsert.getCascadeOperations());
                        pendingInsert.run();
                    }

                    collection.insert(dbObjects.toArray(new DBObject[dbObjects.size()]), writeConcern);
                    for (PendingOperation pendingOperation : postOperations) {
                        pendingOperation.run();
                    }
                    return null;
                }
            });
        }
    }

    public DB getNativeInterface() {
        return ((MongoDatastore)getDatastore()).getMongo().getDB(
             getDocumentMappingContext().getDefaultDatabaseName());
    }

    public DocumentMappingContext getDocumentMappingContext() {
        return (DocumentMappingContext) getMappingContext();
    }

    @Override
    protected Persister createPersister(@SuppressWarnings("rawtypes") Class cls, MappingContext mappingContext) {
        final PersistentEntity entity = mappingContext.getPersistentEntity(cls.getName());
        return entity == null ? null : new MongoEntityPersister(mappingContext, entity, this, publisher);
    }

    @Override
    protected Transaction<DB> beginTransactionInternal() {
        return new SessionOnlyTransaction<DB>(getNativeInterface(), this);
    }

    public MongoTemplate getMongoTemplate(PersistentEntity entity) {
        return mongoDatastore.getMongoTemplate(entity);
    }
}
