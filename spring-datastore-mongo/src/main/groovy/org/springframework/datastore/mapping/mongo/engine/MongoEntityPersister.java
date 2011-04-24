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

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.bson.types.ObjectId;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.data.document.mongodb.DbCallback;
import org.springframework.data.document.mongodb.MongoTemplate;
import org.springframework.datastore.mapping.core.OptimisticLockingException;
import org.springframework.datastore.mapping.engine.AssociationIndexer;
import org.springframework.datastore.mapping.engine.EntityAccess;
import org.springframework.datastore.mapping.engine.NativeEntryEntityPersister;
import org.springframework.datastore.mapping.engine.PropertyValueIndexer;
import org.springframework.datastore.mapping.model.MappingContext;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.model.PersistentProperty;
import org.springframework.datastore.mapping.model.types.Association;
import org.springframework.datastore.mapping.model.types.EmbeddedCollection;
import org.springframework.datastore.mapping.mongo.MongoDatastore;
import org.springframework.datastore.mapping.mongo.MongoSession;
import org.springframework.datastore.mapping.mongo.query.MongoQuery;
import org.springframework.datastore.mapping.query.Query;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.WriteResult;

/**
 * A {@link org.springframework.datastore.mapping.engine.EntityPersister} implementation for the Mongo document store
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class MongoEntityPersister extends NativeEntryEntityPersister<DBObject, Object> {

    private static final String NEXT_ID_SUFFIX = ".next_id";
    private MongoTemplate mongoTemplate;
    private boolean hasNumericalIdentifier = false;
    private boolean hasStringIdentifier = false;

    public static final String MONGO_ID_FIELD = "_id";
    public static final String MONGO_CLASS_FIELD = "_class";

    public MongoEntityPersister(MappingContext mappingContext, PersistentEntity entity,
             MongoSession mongoSession, ApplicationEventPublisher publisher) {
        super(mappingContext, entity, mongoSession, publisher);
        MongoDatastore datastore = (MongoDatastore) mongoSession.getDatastore();
        mongoTemplate = datastore.getMongoTemplate(entity);

        hasNumericalIdentifier = Long.class.isAssignableFrom(entity.getIdentity().getType());
        hasStringIdentifier = String.class.isAssignableFrom(entity.getIdentity().getType());
    }

    @Override
    protected DBObject getEmbedded(DBObject nativeEntry, String key) {
        final Object embeddedDocument = nativeEntry.get(key);
        if (embeddedDocument instanceof DBObject) {
            return (DBObject) embeddedDocument;
        }
        return null;
    }

    @Override
    protected void setEmbedded(DBObject nativeEntry, String key, DBObject embeddedEntry) {
        nativeEntry.put(key, embeddedEntry);
    }

    @Override
    protected void setEmbeddedCollection(DBObject nativeEntry, String key, Collection<?> instances,
            List<DBObject> embeddedEntries) {
        if (instances == null || instances.isEmpty()) {
            return;
        }

        nativeEntry.put(key, embeddedEntries.toArray());
    }

    @Override
    protected void loadEmbeddedCollection(@SuppressWarnings("rawtypes") EmbeddedCollection embeddedCollection,
            EntityAccess ea, Object embeddedInstances, String propertyKey) {

        Collection<Object> instances;
        if (List.class.isAssignableFrom(embeddedCollection.getType())) {
            instances = new ArrayList<Object>();
        }
        else {
            instances = new HashSet<Object>();
        }

        if (embeddedInstances instanceof BasicDBList) {
            BasicDBList list = (BasicDBList)embeddedInstances;
            for (Object dbo : list) {
                if (dbo instanceof BasicDBObject) {
                    BasicDBObject nativeEntry = (BasicDBObject)dbo;
                    String embeddedClassName = (String)nativeEntry.remove("$$embeddedClassName$$");
                    PersistentEntity embeddedPersistentEntity =
                        getMappingContext().getPersistentEntity(embeddedClassName);

                    Object instance = newEntityInstance(embeddedPersistentEntity);
                    refreshObjectStateFromNativeEntry(embeddedPersistentEntity, instance, null, nativeEntry);
                    instances.add(instance);
                }
            }
        }

        ea.setProperty(propertyKey, instances);
    }

    public Query createQuery() {
        return new MongoQuery((MongoSession) getSession(), getPersistentEntity());
    }

    @Override
    protected boolean doesRequirePropertyIndexing() {
        return false;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected List<Object> retrieveAllEntities(PersistentEntity persistentEntity,
            Iterable<Serializable> keys) {

        Query query = session.createQuery(persistentEntity.getJavaClass());

        if (keys instanceof List) {
            query.in(persistentEntity.getIdentity().getName(), (List)keys);
        }
        else {
            List<Serializable> keyList = new ArrayList<Serializable>();
            for (Serializable key : keys) {
                keyList.add(key);
            }
            query.in(persistentEntity.getIdentity().getName(), keyList);
        }

        List<Object> entityResults = new ArrayList<Object>();
        Iterator<Serializable> keyIterator = keys.iterator();
        Iterator<Object> listIterator = query.list().iterator();
        while (keyIterator.hasNext() && listIterator.hasNext()) {
            Serializable key = keyIterator.next();
            Object next = listIterator.next();
            if (next instanceof DBObject) {
                entityResults.add(createObjectFromNativeEntry(getPersistentEntity(), key, (DBObject)next));
            }
            else {
                entityResults.add(next);
            }
        }

        return entityResults;
    }

    @Override
    protected List<Object> retrieveAllEntities(PersistentEntity persistentEntity, Serializable[] keys) {
        return retrieveAllEntities(persistentEntity, Arrays.asList(keys));
    }

    @Override
    public String getEntityFamily() {
        return mongoTemplate.getDefaultCollectionName();
    }

    @Override
    protected void deleteEntry(String family, final Object key) {
        mongoTemplate.execute(new DbCallback<Object>() {
            public Object doInDB(DB con) throws MongoException, DataAccessException {
                DBCollection dbCollection = getCollection(con);

                DBObject dbo = createDBObjectWithKey(key);
                MongoSession mongoSession = (MongoSession) session;
                dbCollection.remove(dbo, mongoSession.getWriteConcern());
                return null;
            }

            protected DBCollection getCollection(DB con) {
                return con.getCollection(getCollectionName(getPersistentEntity()));
            }
        });
    }

    @Override
    protected Object generateIdentifier(final PersistentEntity persistentEntity,
            final DBObject nativeEntry) {
        return mongoTemplate.execute(new DbCallback<Object>() {
            public Object doInDB(DB con) throws MongoException, DataAccessException {

                String collectionName = getCollectionName(persistentEntity, nativeEntry);

                DBCollection dbCollection = con.getCollection(collectionName + NEXT_ID_SUFFIX);

                // If there is a numeric identifier then we need to rely on optimistic concurrency controls to obtain a unique identifer
                // sequence. If the identifier is not numeric then we assume BSON ObjectIds.
                if (hasNumericalIdentifier) {
                    while (true) {
                        DBCursor result = dbCollection.find().sort(new BasicDBObject(MONGO_ID_FIELD, -1)).limit(1);

                        long nextId;
                        if (result.hasNext()) {
                            final Long current = getMappingContext().getConversionService().convert(
                                   result.next().get(MONGO_ID_FIELD), Long.class);
                            nextId = current + 1;
                        }
                        else {
                            nextId = 1;
                        }

                        nativeEntry.put(MONGO_ID_FIELD, nextId);
                        final WriteResult writeResult = dbCollection.insert(nativeEntry);
                        final CommandResult lastError = writeResult.getLastError();
                        if (lastError.ok()) {
                            break;
                        }

                        final Object code = lastError.get("code");
                        // duplicate key error try again
                        if (code != null && code.equals(11000)) {
                            continue;
                        }
                        break;
                    }

                    return nativeEntry.get(MONGO_ID_FIELD);
                }

                ObjectId objectId = ObjectId.get();
                if (ObjectId.class.isAssignableFrom(persistentEntity.getIdentity().getType())) {
                    nativeEntry.put(MONGO_ID_FIELD, objectId);
                    return objectId;
                }

                String stringId = objectId.toString();
                nativeEntry.put(MONGO_ID_FIELD, stringId);
                return stringId;
            }
        });
    }

    @SuppressWarnings("rawtypes")
    @Override
    public PropertyValueIndexer getPropertyIndexer(PersistentProperty property) {
        // We don't need to implement this for Mongo since Mongo automatically creates indexes for us
        return null;
    }

    @Override
    @SuppressWarnings("rawtypes")
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

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static List<? extends Class> convertToString = Arrays.asList(
        BigDecimal.class,
        BigInteger.class,
        Locale.class,
        TimeZone.class,
        Currency.class,
        URL.class);

    @Override
    protected void setEntryValue(DBObject nativeEntry, String key, Object value) {

        // test whether the value can be BSON encoded, if it can't convert to String
        if (value != null && !getMappingContext().isPersistentEntity(value)) {
            if (shouldConvertToString(value.getClass())) {
                value = value.toString();
            }
            else if (value instanceof Calendar) {
                value = ((Calendar)value).getTime();
            }

            nativeEntry.put(key, value);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private boolean shouldConvertToString(Class theClass) {
        for (Class classToCheck : convertToString) {
            if (classToCheck.isAssignableFrom(theClass)) return true;
        }
        return false;
    }

    @Override
    protected PersistentEntity discriminatePersistentEntity(PersistentEntity persistentEntity, DBObject nativeEntry) {
        final Object o = nativeEntry.get(MONGO_CLASS_FIELD);
        if (o != null) {
            final String className = o.toString();
            final PersistentEntity childEntity = getMappingContext().getChildEntityByDiscriminator(persistentEntity.getRootEntity(), className);
            if (childEntity != null) {
                return childEntity;
            }
        }
        return super.discriminatePersistentEntity(persistentEntity, nativeEntry);
    }

    @Override
    protected DBObject retrieveEntry(final PersistentEntity persistentEntity,
            String family, final Serializable key) {
        return mongoTemplate.execute(new DbCallback<DBObject>() {
            public DBObject doInDB(DB con) throws MongoException, DataAccessException {
                DBCollection dbCollection = con.getCollection(getCollectionName(persistentEntity));
                DBObject dbo = createDBObjectWithKey(key);
                return dbCollection.findOne(dbo);
            }
        });
    }

    @Override
    protected Object storeEntry(final PersistentEntity persistentEntity, final EntityAccess entityAccess,
                                final Object storeId, final DBObject nativeEntry) {
        return mongoTemplate.execute(new DbCallback<Object>() {
            public Object doInDB(DB con) throws MongoException, DataAccessException {
                String collectionName = getCollectionName(persistentEntity, nativeEntry);

                DBCollection dbCollection = con.getCollection(collectionName);
                nativeEntry.put(MONGO_ID_FIELD, storeId);
                dbCollection.insert(nativeEntry);
                return nativeEntry.get(MONGO_ID_FIELD);
            }
        });
    }

    public String getCollectionName(PersistentEntity persistentEntity) {
        return getCollectionName(persistentEntity, null);
    }

    private String getCollectionName(PersistentEntity persistentEntity, DBObject nativeEntry) {
        String collectionName;
        if (persistentEntity.isRoot()) {
            collectionName = mongoTemplate.getDefaultCollectionName();
        }
        else {
            MongoSession mongoSession = (MongoSession) getSession();
            collectionName = mongoSession.getMongoTemplate(persistentEntity.getRootEntity()).getDefaultCollectionName();
            if (nativeEntry != null) {
                nativeEntry.put(MONGO_CLASS_FIELD, persistentEntity.getDiscriminator());
            }
        }
        return collectionName;
    }

    @Override
    public void updateEntry(final PersistentEntity persistentEntity, final EntityAccess ea,
            final Object key, final DBObject entry) {
        mongoTemplate.execute(new DbCallback<Object>() {
            public Object doInDB(DB con) throws MongoException, DataAccessException {
                String collectionName = getCollectionName(persistentEntity, entry);
                DBCollection dbCollection = con.getCollection(collectionName);
                DBObject dbo = createDBObjectWithKey(key);

                if (isVersioned(ea)) {
                    // TODO this should be done with a CAS approach if possible
                    DBObject previous = dbCollection.findOne(dbo);
                    checkVersion(ea, previous, persistentEntity, key);
                }

                MongoSession mongoSession = (MongoSession) session;
                dbCollection.update(dbo, entry, false, false, mongoSession.getWriteConcern());
                return null;
            }
        });
    }

    protected void checkVersion(final EntityAccess ea, final DBObject previous,
                                final PersistentEntity persistentEntity, final Object key) {
        Object oldVersion = previous.get("version");
        Object currentVersion = ea.getProperty("version");
        if (Number.class.isAssignableFrom(ea.getPropertyType("version"))) {
            oldVersion = ((Number)oldVersion).longValue();
            currentVersion = ((Number)currentVersion).longValue();
        }
        if (oldVersion != null && currentVersion != null && !oldVersion.equals(currentVersion)) {
            throw new OptimisticLockingException(persistentEntity, key);
        }
        incrementVersion(ea);
    }

    @Override
    protected void deleteEntries(String family, final List<Object> keys) {
        mongoTemplate.execute(new DbCallback<Object>() {
            public Object doInDB(DB con) throws MongoException, DataAccessException {
                String collectionName = getCollectionName(getPersistentEntity());
                DBCollection dbCollection = con.getCollection(collectionName);

                MongoSession mongoSession = (MongoSession) getSession();
                MongoQuery query = mongoSession.createQuery(getPersistentEntity().getJavaClass());
                query.in(getPersistentEntity().getIdentity().getName(), keys);

                dbCollection.remove(query.getMongoQuery());

                return null;
            }
        });
    }

    protected DBObject createDBObjectWithKey(Object key) {
        DBObject dbo = new BasicDBObject();
        if (hasNumericalIdentifier || hasStringIdentifier) {
            dbo.put(MONGO_ID_FIELD, key);
        }
        else {
            if (key instanceof ObjectId) {
                dbo.put(MONGO_ID_FIELD, key);
            }
            else {
                dbo.put(MONGO_ID_FIELD, new ObjectId(key.toString()));
            }
        }
        return dbo;
    }

    @SuppressWarnings("rawtypes")
    private class MongoAssociationIndexer implements AssociationIndexer {
        private DBObject nativeEntry;
        private Association association;
        @SuppressWarnings("hiding") private MongoSession session;

        public MongoAssociationIndexer(DBObject nativeEntry, Association association, MongoSession session) {
            this.nativeEntry = nativeEntry;
            this.association = association;
            this.session = session;
        }

        public void index(final Object primaryKey, List foreignKeys) {
            // if the association is a unidirecitonal one-to-many we stores the keys
            // embedded in the owning entity, otherwise we use a foreign key
            if (!association.isBidirectional()) {
                nativeEntry.put(association.getName(), foreignKeys);
                mongoTemplate.execute(new DbCallback<Object>() {
                    public Object doInDB(DB db) throws MongoException, DataAccessException {
                        final DBCollection collection = db.getCollection(getCollectionName(association.getOwner()));
                        DBObject query = new BasicDBObject(MONGO_ID_FIELD, primaryKey);
                        collection.update(query, nativeEntry);
                        return null;
                    }
                });
            }
        }

        @SuppressWarnings("unchecked")
        public List query(Object primaryKey) {
            // for a unidirectional one-to-many we use the embedded keys
            if (!association.isBidirectional()) {
                final Object indexed = nativeEntry.get(association.getName());
                if (indexed instanceof Collection) {
                    if (indexed instanceof List) return (List) indexed;
                    return new ArrayList((Collection)indexed);
                }
                return Collections.emptyList();
            }
            // for a bidirectional one-to-many we use the foreign key to query the inverse side of the association
            Association inverseSide = association.getInverseSide();
            Query query = session.createQuery(association.getAssociatedEntity().getJavaClass());
            query.eq(inverseSide.getName(), primaryKey);
            query.projections().id();
            return query.list();
        }

        public PersistentEntity getIndexedEntity() {
            return association.getAssociatedEntity();
        }

        public void index(Object primaryKey, Object foreignKey) {
            // TODO: Implement indexing of individual entities
        }
    }
}
