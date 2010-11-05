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
package org.springframework.datastore.mapping.mongo.engine;

import com.mongodb.*;

import org.bson.types.ObjectId;
import org.springframework.datastore.document.DocumentStoreConnectionCallback;
import org.springframework.datastore.document.mongodb.MongoTemplate;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.engine.AssociationIndexer;
import org.springframework.datastore.mapping.engine.NativeEntryEntityPersister;
import org.springframework.datastore.mapping.engine.PropertyValueIndexer;
import org.springframework.datastore.mapping.model.MappingContext;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.model.PersistentProperty;
import org.springframework.datastore.mapping.model.types.Association;
import org.springframework.datastore.mapping.mongo.MongoDatastore;
import org.springframework.datastore.mapping.mongo.MongoSession;
import org.springframework.datastore.mapping.mongo.query.MongoQuery;
import org.springframework.datastore.mapping.query.Query;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * A {@link org.springframework.datastore.mapping.engine.EntityPersister} implementation for the Mongo document store
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class MongoEntityPersister extends NativeEntryEntityPersister<DBObject, Object> {

    public static final String MONGO_ID_FIELD = "_id";
    public static final String MONGO_CLASS_FIELD = "_class";
    private MongoTemplate mongoTemplate;
    private boolean hasNumericalIdentifier = false;

	
	public MongoEntityPersister(MappingContext mappingContext,
			PersistentEntity entity, MongoSession mongoSession) {
		super(mappingContext, entity, mongoSession);
		MongoDatastore datastore = (MongoDatastore) mongoSession.getDatastore();
		this.mongoTemplate = datastore.getMongoTemplate(entity);

        final PersistentProperty identity = entity.getIdentity();
        hasNumericalIdentifier = Long.class.isAssignableFrom(identity.getType());
	}

	public Query createQuery() {
        return new MongoQuery((MongoSession) getSession(), getPersistentEntity());
	}

    @Override
    public String getEntityFamily() {
        return mongoTemplate.getDefaultCollectionName();
    }

    @Override
	protected void deleteEntry(String family, final Object key) {
		mongoTemplate.execute(new DocumentStoreConnectionCallback<DB, Object>() {
			public Object doInConnection(DB con) throws Exception {
				DBCollection dbCollection = con.getCollection(getCollectionName(getPersistentEntity()));
				
				DBObject dbo = new BasicDBObject();
                if(hasNumericalIdentifier) {
                    dbo.put(MONGO_ID_FIELD, key);
                }
                else {
                    dbo.put(MONGO_ID_FIELD, new ObjectId(key.toString()));
                }
				dbCollection.remove(dbo);
				return null;
			}
		});
	}

	@Override
	protected Object generateIdentifier(final PersistentEntity persistentEntity,
			final DBObject nativeEntry) {
		return mongoTemplate.execute(new DocumentStoreConnectionCallback<DB, Object>() {
			public Object doInConnection(DB con) throws Exception {

                String collectionName = getCollectionName(persistentEntity, nativeEntry);

                DBCollection dbCollection = con.getCollection(collectionName);

                // If there is a numeric identifier then we need to rely on optimistic concurrency controls to obtain a unique identifer
                // sequence. If the identifier is not numeric then we assume BSON ObjectIds.
                if(hasNumericalIdentifier) {
                    while (true) {
                        DBCursor result = dbCollection.find().sort(new BasicDBObject(MONGO_ID_FIELD, -1)).limit(1);

                        long nextId;
                        if(result.hasNext()) {
                            final Long current = getMappingContext().getConversionService().convert(result.next().get(MONGO_ID_FIELD), Long.class);
                            nextId = current + 1;
                        }
                        else {
                            nextId = 1;
                        }

                        nativeEntry.put(MONGO_ID_FIELD, nextId);
                        final WriteResult writeResult = dbCollection.insert(nativeEntry);
                        final CommandResult lastError = writeResult.getLastError();
                        if(lastError.ok()) {
                            break;
                        }
                        else {
                            final Object code = lastError.get("code");
                            // duplicate key error try again
                            if(code != null && code.equals(11000)) {
                                continue;
                            }
                            break;
                        }
                    }

                    return nativeEntry.get(MONGO_ID_FIELD);
                }
                else {
                    dbCollection.insert(nativeEntry);
                    ObjectId id = (ObjectId) nativeEntry.get(MONGO_ID_FIELD);
                    return id.toString();
                }
			}
		});
	}


    @Override
	public PropertyValueIndexer getPropertyIndexer(PersistentProperty property) {
		// We don't need to implement this for Mongo since Mongo automatically creates indexes for us
		return null;
	}

	@Override
	public AssociationIndexer getAssociationIndexer(DBObject nativeEntry, Association association) {
		return new MongoAssociationIndexer(nativeEntry, association, (MongoSession) session);
	}

	@Override
	protected DBObject createNewEntry(String family) {
		return new BasicDBObject();
	}

	@Override
	protected Object getEntryValue(DBObject nativeEntry, String property) {
		return nativeEntry.get(property);
	}

    private static List<Class> convertToString = new ArrayList<Class>() {{
        add(BigDecimal.class);
        add(BigInteger.class);
        add(Locale.class);
        add(TimeZone.class);
        add(Currency.class);
        add(URL.class);
    }};

	@Override
	protected void setEntryValue(DBObject nativeEntry, String key, Object value) {

        // test whether the value can be BSON encoded, if it can't convert to String
        if(value != null) {
            if(shouldConvertToString(value.getClass())) {
                value = value.toString();
            }
            else if(value instanceof Calendar) {
                value = ((Calendar)value).getTime();
            }

            nativeEntry.put(key, value);
        }

	}

    private boolean shouldConvertToString(Class theClass) {
        for (Class classToCheck : convertToString) {
            if(classToCheck.isAssignableFrom(theClass)) return true;
        }
        return false;
    }

    @Override
    protected PersistentEntity discriminatePersistentEntity(PersistentEntity persistentEntity, DBObject nativeEntry) {
        final Object o = nativeEntry.get(MONGO_CLASS_FIELD);
        if(o != null) {
            final String className = o.toString();
            final PersistentEntity childEntity = getMappingContext().getChildEntityByDiscriminator(persistentEntity.getRootEntity(), className);
            if(childEntity != null)
                return childEntity;
        }
        return super.discriminatePersistentEntity(persistentEntity, nativeEntry);
    }

    @Override
	protected DBObject retrieveEntry(final PersistentEntity persistentEntity,
			String family, final Serializable key) {
		return (DBObject) mongoTemplate.execute(new DocumentStoreConnectionCallback<DB, Object>() {
			public Object doInConnection(DB con) throws Exception {
				DBCollection dbCollection = con.getCollection(getCollectionName(persistentEntity));
				
				DBObject dbo = new BasicDBObject();
                if(hasNumericalIdentifier) {
                    dbo.put(MONGO_ID_FIELD, key );
                }
                else {
                    dbo.put(MONGO_ID_FIELD, new ObjectId(key.toString()) );
                }
				return dbCollection.findOne(dbo);
				
			}
		});
	}

	@Override
	protected Object storeEntry(final PersistentEntity persistentEntity,
			final Object storeId, final DBObject nativeEntry) {
		return mongoTemplate.execute(new DocumentStoreConnectionCallback<DB, Object>() {
			public Object doInConnection(DB con) throws Exception {

                String collectionName = getCollectionName(persistentEntity, nativeEntry);

                DBCollection dbCollection = con.getCollection(collectionName);
                DBObject idQuery = new BasicDBObject();
                
                idQuery.put(MONGO_ID_FIELD, hasNumericalIdentifier ? storeId : new ObjectId(storeId.toString()));
                dbCollection.update(idQuery, nativeEntry);

                return nativeEntry.get(MONGO_ID_FIELD);
			}
		});
	}

    public String getCollectionName(PersistentEntity persistentEntity) {
        return getCollectionName(persistentEntity, null);
    }

    private String getCollectionName(PersistentEntity persistentEntity, DBObject nativeEntry) {
        String collectionName;
        if(persistentEntity.isRoot()) {
            collectionName = mongoTemplate.getDefaultCollectionName();
        }
        else {
            MongoSession mongoSession = (MongoSession) getSession();
            collectionName = mongoSession.getMongoTemplate(persistentEntity.getRootEntity()).getDefaultCollectionName();
            if(nativeEntry != null) {
                nativeEntry.put(MONGO_CLASS_FIELD, persistentEntity.getDiscriminator());
            }
        }
        return collectionName;
    }

    @Override
	protected void updateEntry(final PersistentEntity persistentEntity, final Object key,
			final DBObject entry) {
		mongoTemplate.execute(new DocumentStoreConnectionCallback<DB, Object>() {
			public Object doInConnection(DB con) throws Exception {
                String collectionName = getCollectionName(persistentEntity, entry);
				DBCollection dbCollection = con.getCollection(collectionName);
				DBObject dbo = new BasicDBObject();
                if(hasNumericalIdentifier) {
                    dbo.put(MONGO_ID_FIELD, key);
                }
                else {
                    dbo.put(MONGO_ID_FIELD, new ObjectId(key.toString()));
                }
				dbCollection.update(dbo, entry);
				return null;
			}
		});
	}

	@Override
	protected void deleteEntries(String family, List<Object> keys) {
		// TODO: Suboptimal. Fix me.
		for (Object key : keys) {
			deleteEntry(family, key);
		}
		
	}


    private class MongoAssociationIndexer implements AssociationIndexer {
        private DBObject nativeEntry;
        private Association assocation;
        private MongoSession session;

        public MongoAssociationIndexer(DBObject nativeEntry, Association association, MongoSession session) {
            this.nativeEntry = nativeEntry;
            this.assocation = association;
            this.session = session;

        }

        public void index(final Object primaryKey, List foreignKeys) {
            nativeEntry.put(assocation.getName(), foreignKeys);
            mongoTemplate.execute(new DocumentStoreConnectionCallback<DB, Object>() {

                public Object doInConnection(DB db) throws Exception {
                    final DBCollection collection = db.getCollection(getCollectionName(assocation.getOwner()));
                    DBObject query = new BasicDBObject(MONGO_ID_FIELD, primaryKey);
                    collection.update(query, nativeEntry);
                    return null;
                }
            });
        }

        public List query(Object primaryKey) {
            final Object indexed = nativeEntry.get(assocation.getName());
            if(indexed instanceof Collection) {
                if(indexed instanceof List) return (List) indexed;
                else {
                    return new ArrayList((Collection)indexed);
                }
            }
            else {
                return Collections.emptyList();
            }
        }

        public PersistentEntity getIndexedEntity() {
            return assocation.getAssociatedEntity();
        }

        public void index(Object primaryKey, Object foreignKey) {
            // TODO: Implement indexing of individual entities
        }
    }
}
