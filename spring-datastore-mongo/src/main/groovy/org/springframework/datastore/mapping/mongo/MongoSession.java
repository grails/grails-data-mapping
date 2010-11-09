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

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

import org.springframework.datastore.document.DocumentStoreConnectionCallback;
import org.springframework.datastore.document.mongodb.MongoTemplate;
import org.springframework.datastore.mapping.core.AbstractSession;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.core.impl.PendingInsert;
import org.springframework.datastore.mapping.core.impl.PendingOperation;
import org.springframework.datastore.mapping.engine.EntityAccess;
import org.springframework.datastore.mapping.engine.Persister;
import org.springframework.datastore.mapping.model.MappingContext;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.mongo.engine.MongoEntityPersister;
import org.springframework.datastore.mapping.transactions.Transaction;
import org.springframework.transaction.TransactionSystemException;

/**
 * A {@link Session} implementation for the Mongo document store
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class MongoSession extends AbstractSession<DB> {

	MongoDatastore mongoDatastore;
	public MongoSession(MongoDatastore datastore, MappingContext mappingContext) {
		super(datastore, mappingContext);
		this.mongoDatastore = datastore;
	}

	public boolean isConnected() {
		// Mongo driver doesn't have a way to check whether a connection is active so we assume it is
		return true;
	}


	@Override
	protected void flushPendingInserts(
			final Map<PersistentEntity, Collection<PendingInsert>> inserts) {
		
		// Optimizes saving multipe entities at once		
		for (final PersistentEntity entity : inserts.keySet()) {
			final MongoTemplate template = getMongoTemplate(entity.isRoot() ? entity : entity.getRootEntity());
			template.execute(new DocumentStoreConnectionCallback<DB, Object>() {

				@Override
				public Object doInConnection(DB db) throws Exception {
					final DBCollection collection = db.getCollection(template.getDefaultCollectionName());
					
					final Collection<PendingInsert> pendingInserts = inserts.get(entity);
					List<DBObject> dbObjects = new LinkedList<DBObject>();
					List<PendingOperation> postOperations = new LinkedList<PendingOperation>();
					
					final MongoEntityPersister persister = (MongoEntityPersister) getPersister(entity);
					
					for (PendingInsert pendingInsert : pendingInserts) {
						final EntityAccess entityAccess = pendingInsert.getEntityAccess();
						if(persister.fireBeforeInsert(entity, entityAccess)) continue;
						
						final List<PendingOperation> preOperations = pendingInsert.getPreOperations();
						for (PendingOperation preOperation : preOperations) {
							preOperation.run();
						}
						
						dbObjects.add((DBObject) pendingInsert.getNativeEntry());
						postOperations.addAll(pendingInsert.getCascadeOperations());
					}
					
					
					collection.insert(dbObjects);
					for (PendingOperation pendingOperation : postOperations) {
						pendingOperation.run();
					}
					return null;
				}
			});
		}
	}
	public DB getNativeInterface() {
		return ((MongoDatastore)getDatastore()).getMongo().getDB("test");
	}

	@Override
	protected Persister createPersister(Class cls, MappingContext mappingContext) {
        final PersistentEntity entity = mappingContext.getPersistentEntity(cls.getName());
        if(entity != null) {
            return new MongoEntityPersister(mappingContext, entity, this);
        }
        return null;

	}
	
	

	@Override
	protected Transaction beginTransactionInternal() {
		throw new TransactionSystemException("Transactions are not supported by Mongo. See http://www.mongodb.org/display/DOCS/Atomic+Operations");
	}

    public MongoTemplate getMongoTemplate(PersistentEntity entity) {
        return mongoDatastore.getMongoTemplate(entity);
    }
}
