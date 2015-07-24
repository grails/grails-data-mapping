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
package org.grails.datastore.mapping.mongo.engine;

import com.mongodb.*;
import org.bson.types.ObjectId;
import org.grails.datastore.mapping.core.IdentityGenerationException;
import org.grails.datastore.mapping.core.OptimisticLockingException;
import org.grails.datastore.mapping.core.SessionImplementor;
import org.grails.datastore.mapping.engine.EntityAccess;
import org.grails.datastore.mapping.engine.internal.MappingUtils;
import org.grails.datastore.mapping.model.EmbeddedPersistentEntity;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.config.GormProperties;
import org.grails.datastore.mapping.model.types.EmbeddedCollection;
import org.grails.datastore.mapping.mongo.MongoSession;
import org.grails.datastore.mapping.mongo.config.MongoMappingContext;
import org.grails.datastore.mapping.mongo.query.MongoQuery;
import org.grails.datastore.mapping.query.Query;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.DbCallback;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.io.Serializable;
import java.util.*;

/**
 * A {@link org.grails.datastore.mapping.engine.EntityPersister} implementation for the Mongo document store
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class MongoEntityPersister extends AbstractMongoObectEntityPersister<DBObject> {

    private static final ValueRetrievalStrategy<DBObject> VALUE_RETRIEVAL_STRATEGY = new ValueRetrievalStrategy<DBObject>() {
        @Override
        public Object getValue(DBObject dbObject, String name) {
            return dbObject.get(name);
        }

        @Override
        public void setValue(DBObject dbObject, String name, Object value) {
            dbObject.put(name, value);
        }
    };

    public MongoEntityPersister(MappingContext mappingContext, PersistentEntity entity,
             MongoSession mongoSession, ApplicationEventPublisher publisher) {
        super(mappingContext, entity, mongoSession, publisher);
        if (!(entity instanceof EmbeddedPersistentEntity)) {

            PersistentProperty identity = entity.getIdentity();
            if (identity != null) {
                hasNumericalIdentifier = Long.class.isAssignableFrom(identity.getType());
                hasStringIdentifier = String.class.isAssignableFrom(identity.getType());
            }
        }
    }

    @Override
    ValueRetrievalStrategy<DBObject> getValueRetrievalStrategy() {
        return VALUE_RETRIEVAL_STRATEGY;
    }



    @Override
    protected void loadEmbeddedCollection(EmbeddedCollection embeddedCollection,
            EntityAccess ea, Object embeddedInstances, String propertyKey) {

        if(Map.class.isAssignableFrom(embeddedCollection.getType())) {
            if(embeddedInstances instanceof DBObject) {
                Map instances = new HashMap();
                DBObject embedded = (DBObject)embeddedInstances;
                for (String key : embedded.keySet()) {
                    Object o = embedded.get(key);
                    if(o instanceof DBObject) {
                        DBObject nativeEntry = (DBObject) o;
                        Object instance =
                                createObjectFromEmbeddedNativeEntry(embeddedCollection.getAssociatedEntity(), nativeEntry);
                        SessionImplementor<DBObject> si = (SessionImplementor<DBObject>)getSession();
                        si.cacheEntry(embeddedCollection.getAssociatedEntity(), createEmbeddedCacheEntryKey(instance), nativeEntry);
                        instances.put(key, instance);
                    }

                }
                ea.setProperty(embeddedCollection.getName(), instances);
            }
        }
        else {
            Collection<Object> instances = MappingUtils.createConcreteCollection(embeddedCollection.getType());

            if (embeddedInstances instanceof Collection) {
                Collection coll = (Collection)embeddedInstances;
                for (Object dbo : coll) {
                    if (dbo instanceof BasicDBObject) {
                        BasicDBObject nativeEntry = (BasicDBObject)dbo;
                        Object instance =
                                createObjectFromEmbeddedNativeEntry(embeddedCollection.getAssociatedEntity(), nativeEntry);
                        SessionImplementor<DBObject> si = (SessionImplementor<DBObject>)getSession();
                        si.cacheEntry(embeddedCollection.getAssociatedEntity(), createEmbeddedCacheEntryKey(instance), nativeEntry);
                        instances.add(instance);
                    }
                }
            }

            ea.setProperty(embeddedCollection.getName(), instances);
        }
    }

    @Override
    protected boolean isEmbeddedEntry(Object entry) {
        return entry instanceof DBObject;
    }

    public Query createQuery() {
        return new MongoQuery((MongoSession) getSession(), getPersistentEntity());
    }


    @Override
    protected void deleteEntry(String family, final Object key, final Object entry) {
        getMongoTemplate().execute(new DbCallback<Object>() {
            public Object doInDB(DB con) throws MongoException, DataAccessException {
                DBCollection dbCollection = getCollection(con);

                DBObject dbo = createDBObjectWithKey(key);
                MongoSession mongoSession = (MongoSession) session;
                WriteConcern writeConcern = mongoSession.getDeclaredWriteConcern(getPersistentEntity());
                if (writeConcern != null) {
                    dbCollection.remove(dbo, writeConcern);
                } else {
                    dbCollection.remove(dbo);
                }
                return null;
            }

            protected DBCollection getCollection(DB con) {
                return con.getCollection(getCollectionName(getPersistentEntity()));
            }
        });
    }

    protected MongoTemplate getMongoTemplate() {
        return getMongoSession().getMongoTemplate(getPersistentEntity());
    }

    @Override
    protected Object generateIdentifier(final PersistentEntity persistentEntity, final DBObject nativeEntry) {
        return getMongoTemplate().execute(new DbCallback<Object>() {
            public Object doInDB(DB con) throws MongoException, DataAccessException {

                String collectionName = getCollectionName(persistentEntity, nativeEntry);

                DBCollection dbCollection = con.getCollection(collectionName + NEXT_ID_SUFFIX);

                // If there is a numeric identifier then we need to rely on optimistic concurrency controls to obtain a unique identifer
                // sequence. If the identifier is not numeric then we assume BSON ObjectIds.
                if (hasNumericalIdentifier) {

                    int attempts = 0;
                    while (true) {
                        DBObject result = dbCollection.findAndModify(new BasicDBObject(MONGO_ID_FIELD, collectionName), null, null, doesRequirePropertyIndexing(), new BasicDBObject("$inc", new BasicDBObject("next_id", 1)), true, true);
                        // result should never be null and we shouldn't come back with an error ,but you never know. We should just retry if this happens...
                        if (result != null) {
                            long nextId = getMappingContext().getConversionService().convert(result.get("next_id"), Long.class);
                            nativeEntry.put(MONGO_ID_FIELD, nextId);
                            break;
                        } else {
                            attempts++;
                            if (attempts > 3) {
                                throw new IdentityGenerationException("Unable to generate identity for [" + persistentEntity.getName() + "] using findAndModify after 3 attempts");
                            }
                        }
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


    @Override
    protected DBObject createNewEntry(String family, Object instance) {
        SessionImplementor<DBObject> si = (SessionImplementor<DBObject>)getSession();

        DBObject dbo = si.getCachedEntry(getPersistentEntity(), createInstanceCacheEntryKey(instance));
        if(dbo != null) {
            return dbo;
        }
        else {
            return super.createNewEntry(family, instance);
        }
    }



    @Override
    protected DBObject createNewEntry(String family) {
        BasicDBObject dbo = new BasicDBObject();
        PersistentEntity persistentEntity = getPersistentEntity();
        if (!persistentEntity.isRoot()) {
            dbo.put(MONGO_CLASS_FIELD, persistentEntity.getDiscriminator());
        }

        return dbo;
    }



    @Override
    protected void setEntryValue(DBObject nativeEntry, String key, Object value) {
        MappingContext mappingContext = getMappingContext();
        setDBObjectValue(nativeEntry, key, value, mappingContext);
    }

    public static void setDBObjectValue(DBObject nativeEntry, String key, Object value, MappingContext mappingContext) {
        Object nativeValue = getSimpleNativePropertyValue(value, mappingContext);
        nativeEntry.put(key, nativeValue);
    }

    /**
     * Convert a value into a type suitable for use in Mongo. Collections and maps are converted recursively. The
     * mapping context is used for the conversion if possible, otherwise toString() is the eventual fallback.
     * @param value The value to convert (or null)
     * @param mappingContext The mapping context.
     * @return The converted value (or null)
     */
    public static Object getSimpleNativePropertyValue(Object value, MappingContext mappingContext) {
        Object nativeValue;

        if (value == null || mappingContext.isPersistentEntity(value)) {
            nativeValue = null;
        } else if (MongoMappingContext.isMongoNativeType(value.getClass())) {
            // easy case, no conversion required.
            // Checked first in case any of these types (such as BasicDBObject) are instances of collections
            // or arrays, etc.!
            nativeValue = value;
        } else if (value.getClass().isArray()) {
            Object[] array = (Object[]) value;
            List<Object> nativeColl = new ArrayList<Object>(array.length);
            for (Object item : array) {
                nativeColl.add(getSimpleNativePropertyValue(item, mappingContext));
            }
            nativeValue = nativeColl;
        } else if (value instanceof Collection) {
            Collection existingColl = (Collection)value;
            List<Object> nativeColl = new ArrayList<Object>(existingColl.size());
            for (Object item : existingColl) {
                nativeColl.add(getSimpleNativePropertyValue(item, mappingContext));
            }
            nativeValue = nativeColl;
        } else if (value instanceof Map) {
            Map<String, Object> existingMap = (Map)value;
            Map<String, Object> newMap = new LinkedHashMap<String, Object>();
            for (Map.Entry<String, Object> entry :existingMap.entrySet()) {
                newMap.put(entry.getKey(), getSimpleNativePropertyValue(entry.getValue(), mappingContext));
            }
            nativeValue = newMap;
        } else {
            nativeValue = convertPrimitiveToNative(value, mappingContext);
        }
        return nativeValue;
    }

    private static Object convertPrimitiveToNative(Object item, MappingContext mappingContext) {
        Object nativeValue;
        if (item != null) {
            ConversionService conversionService = mappingContext.getConversionService();
            // go for toInteger or toString.
            TypeDescriptor itemTypeDescriptor = TypeDescriptor.forObject(item);
            Class<?> itemTypeClass = itemTypeDescriptor.getObjectType();
            if ((itemTypeClass.equals(Integer.class) || itemTypeClass.equals(Short.class)) && conversionService.canConvert(itemTypeDescriptor, TypeDescriptor.valueOf(Integer.class))) {
                nativeValue = conversionService.convert(item, Integer.class);
            } else if (conversionService.canConvert(itemTypeDescriptor, TypeDescriptor.valueOf(String.class))) {
                nativeValue = conversionService.convert(item, String.class);
            } else {
                // fall back if no explicit converter is registered, good for URL, Locale, etc.
                nativeValue = item.toString();
            }
        } else {
            nativeValue = null;
        }
        return nativeValue;
    }

    @Override
    protected DBObject retrieveEntry(final PersistentEntity persistentEntity,
            String family, final Serializable key) {
        return getMongoTemplate().execute(new DbCallback<DBObject>() {
            public DBObject doInDB(DB con) throws MongoException, DataAccessException {
                DBCollection dbCollection = con.getCollection(getCollectionName(persistentEntity));
                return dbCollection.findOne(createDBObjectWithKey(key));
            }
        });
    }

    private DBObject removeNullEntries(DBObject nativeEntry) {
        for (String key : new HashSet<String>(nativeEntry.keySet())) {
            Object o = nativeEntry.get(key);
            if (o == null) {
                nativeEntry.removeField(key);
            } else if (o instanceof Object[]) {
                for (Object o2 : (Object[])o) {
                    if (o2 instanceof DBObject) {
                        removeNullEntries((DBObject)o2);
                    }
                }
            } else if (o instanceof List) {
                for (Object o2 : (List)o) {
                    if (o2 instanceof DBObject) {
                        removeNullEntries((DBObject)o2);
                    }
                }
            } else if (o instanceof DBObject) {
                removeNullEntries((DBObject)o);
            }
        }
        return nativeEntry;
    }

    @Override
    protected Object storeEntry(final PersistentEntity persistentEntity, final EntityAccess entityAccess,
                                final Object storeId, final DBObject nativeEntry) {
        return getMongoTemplate().execute(new DbCallback<Object>() {
            public Object doInDB(DB con) throws MongoException, DataAccessException {
                removeNullEntries(nativeEntry);
                nativeEntry.put(MONGO_ID_FIELD, storeId);
                return nativeEntry.get(MONGO_ID_FIELD);
            }
        });
    }

    @Override
    protected String getCollectionName(PersistentEntity persistentEntity, DBObject nativeEntry)  {
        String collectionName;
        if (persistentEntity.isRoot()) {
            MongoSession mongoSession = (MongoSession) getSession();
            collectionName = mongoSession.getCollectionName(persistentEntity);
        }
        else {
            MongoSession mongoSession = (MongoSession) getSession();
            collectionName = mongoSession.getCollectionName(persistentEntity.getRootEntity());
        }
        return collectionName;
    }

    private DBObject modifyNullsToUnsets(DBObject nativeEntry) {
        DBObject unsets = new BasicDBObject();
        DBObject sets = new BasicDBObject();
        for (String key : nativeEntry.keySet()) {
            Object o = nativeEntry.get(key);
            if (o == null) {
                unsets.put(key, 1);
            } else if ("_id".equals(key)) {
            } else if (o instanceof Object[]) {
                sets.put(key, o);
                for (Object o2 : (Object[])o) {
                    if (o2 instanceof DBObject) {
                        removeNullEntries((DBObject)o2);
                    }
                }
            } else if (o instanceof List) {
                sets.put(key, o);
                for (Object o2 : (List)o) {
                    if (o2 instanceof DBObject) {
                        removeNullEntries((DBObject)o2);
                    }
                }
            } else if (o instanceof DBObject) {
                sets.put(key, removeNullEntries((DBObject)o));
            } else {
                sets.put(key, o);
            }
        }
        DBObject newEntry = new BasicDBObject();
        newEntry.put("$set", sets);
        if (!unsets.keySet().isEmpty()) {
            newEntry.put("$unset", unsets);
        }
        return newEntry;
    }

    @Override
    public void updateEntry(final PersistentEntity persistentEntity, final EntityAccess ea,
            final Object key, final DBObject entry) {
        getMongoTemplate().execute(new DbCallback<Object>() {
            public Object doInDB(DB con) throws MongoException, DataAccessException {
                String collectionName = getCollectionName(persistentEntity, entry);
                DBCollection dbCollection = con.getCollection(collectionName);
                DBObject dbo = createDBObjectWithKey(key);

                boolean versioned = isVersioned(ea);
                Object currentVersion = null;
                if (versioned) {
                    currentVersion = getCurrentVersion(ea);
                    incrementVersion(ea);
                    // query for old version to ensure atomicity
                    if (currentVersion != null) {
                        dbo.put(GormProperties.VERSION, currentVersion);
                    }
                }

                DBObject newEntry = modifyNullsToUnsets(entry);

                MongoSession mongoSession = (MongoSession) session;
                WriteConcern writeConcern = mongoSession.getDeclaredWriteConcern(getPersistentEntity());
                WriteResult result;
                if (writeConcern != null) {
                    result = dbCollection.update(dbo, newEntry, false, false, writeConcern);
                }
                else {
                    result = dbCollection.update(dbo, newEntry, false, false);
                }
                if (versioned && !((SessionImplementor)getSession()).isStateless(persistentEntity)) {
                    // ok, we need to check whether the write worked:
                    // note that this will use the standard write concern unless it wasn't at least ACKNOWLEDGE:
                    // if the document count "n" of the update was 0, the version check must have failed:
                    if (result.wasAcknowledged() && result.getN() == 0) {
                        if(currentVersion != null) {
                            ea.setProperty(GormProperties.VERSION, currentVersion);
                        }
                        throw new OptimisticLockingException(persistentEntity, key);
                    }
                }
                return null;
            }
        });
    }



    @Override
    protected void deleteEntries(String family, final List<Object> keys) {
        getMongoTemplate().execute(new DbCallback<Object>() {
            public Object doInDB(DB con) throws MongoException, DataAccessException {
                String collectionName = getCollectionName(getPersistentEntity());
                DBCollection dbCollection = con.getCollection(collectionName);

                MongoSession mongoSession = (MongoSession) getSession();
                MongoQuery query = (MongoQuery) mongoSession.createQuery(getPersistentEntity().getJavaClass());
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

}
