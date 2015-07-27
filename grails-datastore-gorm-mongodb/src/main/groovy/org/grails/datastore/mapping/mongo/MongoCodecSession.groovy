/* Copyright (C) 2015 original authors
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
package org.grails.datastore.mapping.mongo
import com.mongodb.MongoClient
import com.mongodb.WriteConcern
import com.mongodb.bulk.BulkWriteResult
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.DeleteManyModel
import com.mongodb.client.model.InsertOneModel
import com.mongodb.client.model.UpdateOneModel
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.WriteModel
import groovy.transform.CompileStatic
import org.bson.Document
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.configuration.CodecRegistry
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.core.OptimisticLockingException
import org.grails.datastore.mapping.core.impl.PendingDelete
import org.grails.datastore.mapping.core.impl.PendingInsert
import org.grails.datastore.mapping.core.impl.PendingOperation
import org.grails.datastore.mapping.core.impl.PendingUpdate
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import org.grails.datastore.mapping.dirty.checking.DirtyCheckingSupport
import org.grails.datastore.mapping.document.config.DocumentMappingContext
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.config.GormProperties
import org.grails.datastore.mapping.mongo.engine.MongoCodecEntityPersister
import org.grails.datastore.mapping.mongo.engine.MongoEntityPersister
import org.grails.datastore.mapping.mongo.query.MongoQuery
import org.grails.datastore.mapping.transactions.Transaction
import org.springframework.context.ApplicationEventPublisher
import org.springframework.dao.DataIntegrityViolationException

import javax.persistence.FlushModeType
import java.util.concurrent.ConcurrentHashMap
/**
 * A MongoDB session for codec mapping style
 *
 * @author Graeme Rocher
 * @since 4.1
 */
@CompileStatic
class MongoCodecSession extends AbstractMongoSession {
    protected Map<Class, MongoCodecEntityPersister> mongoCodecEntityPersisterMap = new ConcurrentHashMap<Class, MongoCodecEntityPersister>().withDefault { Class type ->
        def context = getDocumentMappingContext()
        def entity = context.getPersistentEntity(type.name)
        if(entity) {
            return new MongoCodecEntityPersister(context, entity, this, publisher, cacheAdapterRepository )
        }
        return null
    }

    public MongoCodecSession(MongoDatastore datastore, MappingContext mappingContext, ApplicationEventPublisher publisher) {
        this(datastore, mappingContext, publisher, false);
    }
    public MongoCodecSession(MongoDatastore datastore, MappingContext mappingContext, ApplicationEventPublisher publisher, boolean stateless) {
        super(datastore, mappingContext, publisher, stateless);
    }

    @Override
    MongoDatastore getDatastore() {
        return (MongoDatastore)super.getDatastore()
    }

    @Override
    void flush(WriteConcern writeConcern) {
        WriteConcern currentWriteConcern = this.getWriteConcern();
        try {
            this.writeConcern = writeConcern;
            final Map<PersistentEntity, Collection<PendingUpdate>> pendingUpdates = getPendingUpdates();
            final Map<PersistentEntity, Collection<PendingInsert>> pendingInserts = getPendingInserts();
            final Map<PersistentEntity, Collection<PendingDelete>> pendingDeletes = getPendingDeletes();

            if(pendingUpdates.isEmpty() && pendingInserts.isEmpty() && pendingDeletes.isEmpty()) {
                return;
            }


            Map<String,Integer> numberOfOptimisticUpdates = [:].withDefault { 0 }
            Map<String,Integer> numberOfPessimisticUpdates = [:].withDefault { 0 }

            Map<PersistentEntity, List<WriteModel<Document>>> writeModels = [:]
            for (PersistentEntity persistentEntity in pendingInserts.keySet()) {
                final Collection<PendingInsert> inserts = pendingInserts[persistentEntity]
                if(inserts) {
                    List<WriteModel<?>> entityWrites = getWriteModelsForEntity(persistentEntity, writeModels)
                    for (PendingInsert insert in inserts) {
                        insert.run()

                        if(insert.vetoed) continue

                        entityWrites << new InsertOneModel<?>( insert.nativeEntry )

                        final List<PendingOperation> cascadeOperations = insert.cascadeOperations
                        addPostFlushOperations cascadeOperations
                    }
                }
            }


            for (PersistentEntity persistentEntity in pendingUpdates.keySet()) {

                final String name = persistentEntity.isRoot() ? persistentEntity.name : persistentEntity.rootEntity.name

                final Collection<PendingUpdate> updates = pendingUpdates[persistentEntity]
                if(updates) {
                    List<WriteModel<?>> entityWrites = getWriteModelsForEntity(persistentEntity, writeModels);
                    for (PendingUpdate update in updates) {
                        update.run()

                        if(update.vetoed) continue

                        DirtyCheckable changedObject = (DirtyCheckable) update.getNativeEntry()
                        def changedProperties = changedObject.listDirtyPropertyNames()
                        Document updateDoc = new Document()
                        def entityAccess = update.entityAccess
                        Document sets = new Document()
                        Document unsets = new Document()
                        for(prop in changedProperties) {
                            if(persistentEntity.getPropertyByName(prop) == null) continue
                            def v = entityAccess.getProperty(prop)
                            if(v == null) {
                                unsets[prop] = v
                            }
                            else {
                                sets[prop] = v
                            }
                        }

                        if(sets) {
                            updateDoc['$set'] = sets
                        }
                        if(unsets) {
                            updateDoc['$unset'] = unsets
                        }

                        final Object nativeKey = update.nativeKey
                        final Document id = new Document(MongoEntityPersister.MONGO_ID_FIELD, nativeKey)
                        MongoCodecEntityPersister documentEntityPersister = (MongoCodecEntityPersister) getPersister(persistentEntity);
                        if(documentEntityPersister.isVersioned(entityAccess)) {
                            Object currentVersion = documentEntityPersister.getCurrentVersion(entityAccess);
                            documentEntityPersister.incrementVersion(entityAccess);

                            // if the entity is versioned we add to the query the current version
                            // if the query doesn't match a result this means the document has been updated by
                            // another thread an an optimistic locking exception should be thrown
                            id[GormProperties.VERSION] = currentVersion
                            numberOfOptimisticUpdates[name]++
                        }
                        else {
                            numberOfPessimisticUpdates[name]++
                        }
                        final UpdateOptions options = new UpdateOptions();

                        entityWrites.add(new UpdateOneModel<Document>(id, updateDoc, options.upsert(false)));

                        final List cascadeOperations = update.getCascadeOperations();
                        addPostFlushOperations(cascadeOperations);
                    }
                }
            }


            for (PersistentEntity persistentEntity in pendingDeletes.keySet()) {
                final Collection<PendingDelete> deletes = pendingDeletes[persistentEntity]
                if(deletes) {
                    List<WriteModel<?>> entityWrites = getWriteModelsForEntity(persistentEntity, writeModels)
                    List<Object> nativeKeys = []
                    for (PendingDelete delete in deletes) {
                        delete.run()

                        if(delete.vetoed) continue

                        final Object k = delete.nativeKey
                        if(k) {
                            nativeKeys << k
                            final List cascadeOperations = delete.cascadeOperations
                            addPostFlushOperations cascadeOperations
                        }

                    }
                    entityWrites<< new DeleteManyModel<Document>(new Document( MongoEntityPersister.MONGO_ID_FIELD, new Document(MongoQuery.MONGO_IN_OPERATOR, nativeKeys)))
                }
            }


            for (PersistentEntity persistentEntity : writeModels.keySet()) {
                MongoCollection collection = getCollection(persistentEntity)
                                                .withDocumentClass(persistentEntity.javaClass)

                collection = collection.withCodecRegistry( mongoDatastore.codecRegistry )
                final WriteConcern wc = writeConcern
                if(wc) {
                    collection = collection.withWriteConcern(wc)
                }
                final List<WriteModel<?>> writes = writeModels[persistentEntity]
                if(writes) {

                    final BulkWriteResult bulkWriteResult = collection
                                                                .bulkWrite(writes)

                    if( !bulkWriteResult.wasAcknowledged() ) {
                        errorOccured = true;
                        throw new DataIntegrityViolationException("Write operation was not acknowledged");
                    }
                    else {
                        final int matchedCount = bulkWriteResult.matchedCount
                        final String name = persistentEntity.name
                        final Integer numOptimistic = numberOfOptimisticUpdates[name]
                        final Integer numPessimistic = numberOfPessimisticUpdates[name]
                        if((matchedCount - numPessimistic) != numOptimistic) {
                            setFlushMode(FlushModeType.COMMIT)
                            throw new OptimisticLockingException(persistentEntity, null)
                        }
                    }
                }
            }

            for (Runnable postFlushOperation : postFlushOperations) {
                postFlushOperation.run();
            }
        } finally {
            clearPendingOperations();
            postFlushOperations.clear();
            firstLevelCollectionCache.clear();
            this.writeConcern = currentWriteConcern;
        }
    }

    @Override
    MongoClient getNativeInterface() {
        return mongoDatastore.mongoClient
    }

    public DocumentMappingContext getDocumentMappingContext() {
        return (DocumentMappingContext) getMappingContext()
    }

    protected List<WriteModel<?>> getWriteModelsForEntity(PersistentEntity persistentEntity, Map<PersistentEntity, List<WriteModel<?>>> writeModels) {
        PersistentEntity key = persistentEntity.root ? persistentEntity : persistentEntity.rootEntity
        List<WriteModel<?>> entityWrites = writeModels[key]
        if(entityWrites == null) {
            entityWrites = new ArrayList<WriteModel<?>>()
            writeModels[key] = entityWrites
        }
        return entityWrites
    }

    @Override
    protected Transaction beginTransactionInternal() {
        throw new UnsupportedOperationException("MongoDB does not support transactions")
    }

    @Override
    protected MongoCodecEntityPersister createPersister(Class cls, MappingContext mappingContext) {
        return mongoCodecEntityPersisterMap[cls]
    }

}
