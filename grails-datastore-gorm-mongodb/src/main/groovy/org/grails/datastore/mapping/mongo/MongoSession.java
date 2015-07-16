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

package org.grails.datastore.mapping.mongo;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.grails.datastore.mapping.core.AbstractSession;
import org.grails.datastore.mapping.core.impl.PendingInsert;
import org.grails.datastore.mapping.core.impl.PendingOperation;
import org.grails.datastore.mapping.document.config.DocumentMappingContext;
import org.grails.datastore.mapping.engine.Persister;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.mongo.config.MongoCollection;
import org.grails.datastore.mapping.mongo.engine.MongoEntityPersister;
import org.grails.datastore.mapping.mongo.query.MongoQuery;
import org.grails.datastore.mapping.query.Query;
import org.grails.datastore.mapping.query.api.QueryableCriteria;
import org.grails.datastore.mapping.transactions.SessionOnlyTransaction;
import org.grails.datastore.mapping.transactions.Transaction;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mongodb.core.DbCallback;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;

/**
 * A {@link org.grails.datastore.mapping.core.Session} implementation for the Mongo document store.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class MongoSession extends AbstractSession<DB> {

    private static final Map<PersistentEntity, WriteConcern> declaredWriteConcerns = new ConcurrentHashMap<PersistentEntity, WriteConcern>();
    MongoDatastore mongoDatastore;
    private WriteConcern writeConcern = null;
    private boolean errorOccured = false;
    protected Map<PersistentEntity, MongoTemplate> mongoTemplates = new ConcurrentHashMap<PersistentEntity, MongoTemplate>();
    protected Map<PersistentEntity, String> mongoCollections = new ConcurrentHashMap<PersistentEntity, String>();


    public MongoSession(MongoDatastore datastore, MappingContext mappingContext, ApplicationEventPublisher publisher) {
        this(datastore, mappingContext, publisher, false);
    }
    public MongoSession(MongoDatastore datastore, MappingContext mappingContext, ApplicationEventPublisher publisher, boolean stateless) {
        super(datastore, mappingContext, publisher, stateless);
        mongoDatastore = datastore;
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
    public void flush(WriteConcern writeConcern) {
        WriteConcern current = this.writeConcern;

        this.writeConcern = writeConcern;

        try {
            if (!errorOccured) {
                super.flush();
            }
        }
        finally {
            this.writeConcern = current;
        }
    }

    @Override
    public void flush() {
        if (!errorOccured) {
            super.flush();
        }
    }

    @Override
    public void disconnect() {
        super.disconnect();
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void flushPendingInserts(final Map<PersistentEntity, Collection<PendingInsert>> inserts) {
        // Optimizes saving multipe entities at once
        for (final PersistentEntity entity : inserts.keySet()) {
            final MongoTemplate template = getMongoTemplate(entity.isRoot() ? entity : entity.getRootEntity());
            final String collectionNameToUse = getCollectionName(entity.isRoot() ? entity : entity.getRootEntity());
            template.execute(new DbCallback<Object>() {
                public Object doInDB(DB db) throws MongoException, DataAccessException {

                    WriteConcern writeConcernToUse = getDeclaredWriteConcern(entity);
                    final DBCollection collection = db.getCollection(collectionNameToUse);

                    final Collection<PendingInsert> pendingInserts = inserts.get(entity);
                    List<DBObject> dbObjects = new LinkedList<DBObject>();
                    List<PendingOperation> postOperations = new LinkedList<PendingOperation>();

                    for (PendingInsert pendingInsert : pendingInserts) {

                        final List<PendingOperation> preOperations = pendingInsert.getPreOperations();
                        for (PendingOperation preOperation : preOperations) {
                            preOperation.run();
                        }


                        pendingInsert.run();
                        if(!pendingInsert.isVetoed()) {
                            dbObjects.add((DBObject) pendingInsert.getNativeEntry());
                            postOperations.addAll(pendingInsert.getCascadeOperations());
                        }
                    }

                    WriteResult writeResult = writeConcernToUse != null ? collection.insert(dbObjects.toArray(new DBObject[dbObjects.size()]), writeConcernToUse )
                                                                            : collection.insert(dbObjects.toArray(new DBObject[dbObjects.size()]));
                    if (!writeResult.wasAcknowledged()) {
                        errorOccured = true;
                        throw new DataIntegrityViolationException("Write operation was not acknowledged");
                    }
                    for (PendingOperation pendingOperation : postOperations) {
                        pendingOperation.run();
                    }
                    return null;
                }
            });
        }
    }

    public WriteConcern getDeclaredWriteConcern(PersistentEntity entity) {
        return getDeclaredWriteConcern(this.writeConcern, entity);
    }

    private WriteConcern getDeclaredWriteConcern(WriteConcern defaultConcern, PersistentEntity entity) {
        WriteConcern writeConcern = declaredWriteConcerns.get(entity);
        if (writeConcern == null) {
            Object mappedForm = entity.getMapping().getMappedForm();
            if (mappedForm instanceof MongoCollection) {
                MongoCollection mc = (MongoCollection) mappedForm;
                writeConcern = mc.getWriteConcern();
                if (writeConcern == null) {
                    writeConcern = defaultConcern;
                }
            }

            if (writeConcern != null) {
                declaredWriteConcerns.put(entity, writeConcern);
            }
        }
        return writeConcern;
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
        MongoTemplate mongoTemplate = mongoTemplates.get(entity);
        return mongoTemplate != null ? mongoTemplate : mongoDatastore.getMongoTemplate(entity);
    }

    public String getCollectionName(PersistentEntity entity) {
        return mongoCollections.containsKey(entity) ? mongoCollections.get(entity) : mongoDatastore.getCollectionName(entity);
    }

    /**
     * Use the given collection for the given entity
     *
     * @param entity The entity
     * @param collectionName The collection
     * @return The previous collection that was used
     */
    public String useCollection(PersistentEntity entity, String collectionName) {
        String current = mongoCollections.containsKey(entity) ? mongoCollections.get(entity) : mongoDatastore.getCollectionName(entity);
        mongoCollections.put(entity, collectionName);
        return current;
    }

    /**
     * Use the given database name for the given entity
     *
     * @param entity The entity name
     * @param databaseName The database name
     * @return The name of the previous database
     */
    public String useDatabase(PersistentEntity entity, String databaseName) {
        MongoTemplate currentTemplate = mongoTemplates.containsKey(entity) ? mongoTemplates.get(entity) : mongoDatastore.getMongoTemplate(entity);
        String currentDatabase = currentTemplate.getDb().getName();
        mongoTemplates.put(entity, new MongoTemplate(mongoDatastore.getMongo(), databaseName, mongoDatastore.getUserCrentials()));
        return currentDatabase;
    }

    @Override
    public int deleteAll(QueryableCriteria criteria) {

        final PersistentEntity entity = criteria.getPersistentEntity();
        final DBObject nativeQuery = buildNativeQueryFromCriteria(criteria, entity);

        pendingDeletes.add(new Runnable() {
            @Override
            public void run() {
                String collectionName = getCollectionName(entity);
                WriteConcern writeConcern = getDeclaredWriteConcern(entity);
                if(writeConcern != null) {
                    getNativeInterface().getCollection(collectionName).remove(nativeQuery, writeConcern);
                }
                else {
                    getNativeInterface().getCollection(collectionName).remove(nativeQuery);
                }
            }
        });

        // not possible to return number of deleted items with MongoDB API
        return -1;
    }

    @Override
    public void delete(final Iterable objects) {
        // sort the objects into sets by Persister, in case the objects are of different types.
        Map<PersistentEntity, List> toDelete = new HashMap<PersistentEntity, List>();
        for (Object object : objects) {
            if (object == null) {
                continue;
            }
            final PersistentEntity p = getMappingContext().getPersistentEntity(object.getClass().getName());
            if (p == null) {
                continue;
            }
            List listForPersister = toDelete.get(p);
            if (listForPersister == null) {
                toDelete.put(p, listForPersister = new ArrayList());
            }
            Serializable id = getObjectIdentifier(object);
            if(id != null) {
                listForPersister.add(id);
            }
        }

        Set<PersistentEntity> persistentEntities = toDelete.keySet();
        for (final PersistentEntity persistentEntity : persistentEntities) {
            final List identifiers = toDelete.get(persistentEntity);
            if(identifiers != null && !identifiers.isEmpty()) {
                pendingDeletes.add(new Runnable() {
                    @Override
                    public void run() {
                        String collectionName = getCollectionName(persistentEntity);
                        DBObject nativeQuery = new BasicDBObject();
                        nativeQuery.put( MongoEntityPersister.MONGO_ID_FIELD, new BasicDBObject(MongoQuery.MONGO_IN_OPERATOR, identifiers));
                        WriteConcern writeConcern = getDeclaredWriteConcern(persistentEntity);
                        if(writeConcern != null) {
                            getNativeInterface().getCollection(collectionName).remove(nativeQuery, writeConcern);
                        }
                        else {
                            getNativeInterface().getCollection(collectionName).remove(nativeQuery);
                        }
                    }
                });

            }
        }
    }


    @Override
    public int updateAll(QueryableCriteria criteria, final Map<String, Object> properties) {
        final PersistentEntity entity = criteria.getPersistentEntity();
        final DBObject nativeQuery = buildNativeQueryFromCriteria(criteria, entity);


        postFlushOperations.add(new Runnable() {
            @Override
            public void run() {
                String collectionName = getCollectionName(entity);
                WriteConcern writeConcern = getDeclaredWriteConcern(entity);
                if(writeConcern != null) {
                    getNativeInterface()
                            .getCollection(collectionName)
                            .update(nativeQuery, new BasicDBObject("$set", properties), false, true, writeConcern);
                }
                else {
                    getNativeInterface()
                            .getCollection(collectionName)
                            .update(nativeQuery, new BasicDBObject("$set", properties), false, true);
                }
            }
        });
        return -1;
    }

    @Override
    protected void cacheEntry(Serializable key, Object entry, Map<Serializable, Object> entryCache, boolean forDirtyCheck) {
        if (forDirtyCheck) {
            entryCache.put(key, new BasicDBObject(((DBObject)entry).toMap()));
        }
        else {
            entryCache.put(key, entry);
        }
    }


    private DBObject buildNativeQueryFromCriteria(QueryableCriteria criteria, PersistentEntity entity) {
        MongoQuery mongoQuery = new MongoQuery(this, entity);
        List<Query.Criterion> criteriaList = criteria.getCriteria();

        for(Query.Criterion c : criteriaList) {
            mongoQuery.add(c);
        }

        return mongoQuery.getMongoQuery();
    }
}
