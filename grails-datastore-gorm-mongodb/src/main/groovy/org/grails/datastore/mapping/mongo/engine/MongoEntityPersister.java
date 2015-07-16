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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.grails.datastore.mapping.core.IdentityGenerationException;
import org.grails.datastore.mapping.core.OptimisticLockingException;
import org.grails.datastore.mapping.core.SessionImplementor;
import org.grails.datastore.mapping.engine.AssociationIndexer;
import org.grails.datastore.mapping.engine.EntityAccess;
import org.grails.datastore.mapping.engine.NativeEntryEntityPersister;
import org.grails.datastore.mapping.engine.Persister;
import org.grails.datastore.mapping.engine.PropertyValueIndexer;
import org.grails.datastore.mapping.engine.internal.MappingUtils;
import org.grails.datastore.mapping.model.EmbeddedPersistentEntity;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.PropertyMapping;
import org.grails.datastore.mapping.model.config.GormProperties;
import org.grails.datastore.mapping.model.types.Association;
import org.grails.datastore.mapping.model.types.EmbeddedCollection;
import org.grails.datastore.mapping.model.types.Identity;
import org.grails.datastore.mapping.model.types.ManyToMany;
import org.grails.datastore.mapping.mongo.MongoDatastore;
import org.grails.datastore.mapping.mongo.MongoSession;
import org.grails.datastore.mapping.mongo.config.MongoAttribute;
import org.grails.datastore.mapping.mongo.config.MongoMappingContext;
import org.grails.datastore.mapping.mongo.query.MongoQuery;
import org.grails.datastore.mapping.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.DbCallback;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;

/**
 * A {@link org.grails.datastore.mapping.engine.EntityPersister} implementation for the Mongo document store
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class MongoEntityPersister extends NativeEntryEntityPersister<DBObject, Object> {

    public static final String INSTANCE_PREFIX = "instance:";
    static Logger log = LoggerFactory.getLogger(MongoEntityPersister.class);

    private static final String NEXT_ID_SUFFIX = ".next_id";
    private boolean hasNumericalIdentifier = false;
    private boolean hasStringIdentifier = false;

    public static final String MONGO_ID_FIELD = "_id";
    public static final String MONGO_CLASS_FIELD = "_class";

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
    protected void refreshObjectStateFromNativeEntry(PersistentEntity persistentEntity, Object obj, Serializable nativeKey, DBObject nativeEntry, boolean isEmbedded) {
        if (isEmbedded) {
            Object id = nativeEntry.get(MONGO_ID_FIELD);
            super.refreshObjectStateFromNativeEntry(persistentEntity, obj, (Serializable) id, nativeEntry, isEmbedded);
        }
        else {
            super.refreshObjectStateFromNativeEntry(persistentEntity, obj, nativeKey, nativeEntry, isEmbedded);
        }
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
    protected void setEmbeddedCollection(final DBObject nativeEntry, final String key, Collection<?> instances, List<DBObject> embeddedEntries) {
        if (instances == null || instances.isEmpty()) {
            nativeEntry.put(key, null);
            return;
        }

        nativeEntry.put(key, embeddedEntries);
    }

    @Override
    protected void setEmbeddedMap(DBObject nativeEntry, String key, Map instances, Map<Object, DBObject> embeddedEntries) {
        if (instances == null || instances.isEmpty()) {
            nativeEntry.put(key, null);
            return;
        }

        nativeEntry.put(key, embeddedEntries);
    }

    /**
     * Implementors who want to support one-to-many associations embedded should implement this method
     *
     * @param association The association
     * @param ea
     * @param nativeEntry
     * @return A list of keys loaded from the embedded instance
     */
    @Override
    protected List loadEmbeddedCollectionKeys(Association association, EntityAccess ea, DBObject nativeEntry) {
        if (nativeEntry == null) {
            return super.loadEmbeddedCollectionKeys(association, ea, nativeEntry);
        }

        Object entry = nativeEntry.get(getPropertyKey(association));
        List keys = new ArrayList();
        if (entry instanceof List) {
            List entries = (List) entry;
            for (Object o : entries) {
                if (o instanceof DBRef) {
                    DBRef dbref = (DBRef) o;
                    keys.add(dbref.getId());
                }
                else if (o != null) {
                    keys.add(o);
                }
                else {
                    keys.add(null);
                }
            }
        }
        return keys;
    }

    @Override
    protected void setEmbeddedCollectionKeys(Association association, EntityAccess embeddedEntityAccess, DBObject embeddedEntry, List<Serializable> keys) {
        List dbRefs = new ArrayList();
        boolean reference = isReference(association);
        for (Object foreignKey : keys) {
            if (reference) {
                dbRefs.add(new DBRef(getCollectionName(association.getAssociatedEntity()), foreignKey));
            } else {
                dbRefs.add(foreignKey);
            }
        }
        embeddedEntry.put(association.getName(), dbRefs);
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
    protected boolean doesRequirePropertyIndexing() {
        return false;
    }

    @Override
    protected List<Object> retrieveAllEntities(PersistentEntity persistentEntity,
            Iterable<Serializable> keys) {

        Query query = session.createQuery(persistentEntity.getJavaClass());

        PersistentProperty identity = persistentEntity.getIdentity();
        if (keys instanceof List) {
            List actualKeys = new ArrayList();
            Iterator iterator = keys.iterator();
            while (iterator.hasNext()) {
                Object key = iterator.next();
                Object id = getIdentifierForKey(key);
                actualKeys.add(id);

            }
            query.in(identity.getName(), actualKeys);
        }
        else {
            List<Serializable> keyList = new ArrayList<Serializable>();
            for (Serializable key : keys) {
                keyList.add(key);
            }
            query.in(identity.getName(), keyList);
        }

        List<Object> entityResults = new ArrayList<Object>();
        Iterator<Serializable> keyIterator = keys.iterator();
        Map<Serializable, Object> resultMap = new HashMap<Serializable, Object>();
        for (Object o : query.list()) {
            if (o instanceof DBObject) {
                DBObject dbo = (DBObject) o;
                o = createObjectFromNativeEntry(getPersistentEntity(), (Serializable) dbo.get(MONGO_ID_FIELD), dbo);
            }
            resultMap.put(getObjectIdentifier(o), o);
        }
        while (keyIterator.hasNext()) {
            Object key = getIdentifierForKey(keyIterator.next());
            ConversionService conversionService = getMappingContext().getConversionService();
            key = conversionService.convert(key, identity.getType());
            Object o = resultMap.get(key);
            entityResults.add(o); // may add null, so entityResults list size matches input list size.
        }

        return entityResults;
    }

    private Object getIdentifierForKey(Object key) {
        Object id = key;
        if (key instanceof DBRef) {
            DBRef ref = (DBRef) key;
            id = ref.getId();
        }
        return id;
    }

    @Override
    protected List<Object> retrieveAllEntities(PersistentEntity persistentEntity, Serializable[] keys) {
        return retrieveAllEntities(persistentEntity, Arrays.asList(keys));
    }

    @Override
    public String getEntityFamily() {
        return getMongoSession().getCollectionName(getPersistentEntity());
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
                        DBObject result = dbCollection.findAndModify(new BasicDBObject(MONGO_ID_FIELD, collectionName), null, null, false, new BasicDBObject("$inc", new BasicDBObject("next_id", 1)), true, true);
                        // result should never be null and we shouldn't come back with an error ,but you never know. We should just retry if this happens...
                        if (result != null) {
                            long nextId = getMappingContext().getConversionService().convert(result.get("next_id"), Long.class);
                            nativeEntry.put(MONGO_ID_FIELD, nextId);
                            break;
                        } else {
                            attempts++;
                            if (attempts > 3) {
                                throw new IdentityGenerationException("Unable to generate identity for ["+persistentEntity.getName()+"] using findAndModify after 3 attempts");
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
    public PropertyValueIndexer getPropertyIndexer(PersistentProperty property) {
        // We don't need to implement this for Mongo since Mongo automatically creates indexes for us
        return null;
    }

    @Override
    public AssociationIndexer getAssociationIndexer(DBObject nativeEntry, Association association) {
        return new MongoAssociationIndexer(nativeEntry, association, (MongoSession) session);
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

    public static String createInstanceCacheEntryKey(Object instance) {
        return INSTANCE_PREFIX + System.identityHashCode(instance);
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
    protected Object getEntryValue(DBObject nativeEntry, String property) {
        Object value = nativeEntry.get(property);
        if (value instanceof DBRef) {
            return getIdentifierForKey(value);
        }
        return value;
    }

    @Override
    protected Object formulateDatabaseReference(PersistentEntity persistentEntity, Association association, Serializable associationId) {
        boolean isReference = isReference(association);
        if (isReference) {
            return new DBRef(getCollectionName(association.getAssociatedEntity()), associationId);
        }
        return associationId;
    }

    private boolean isReference(Association association) {
        PropertyMapping mapping = association.getMapping();
        if (mapping != null) {
            MongoAttribute attribute = (MongoAttribute) mapping.getMappedForm();
            if (attribute != null) {
                return attribute.isReference();
            }
        }
        return true;
    }

    @Override
    protected void setEntryValue(DBObject nativeEntry, String key, Object value) {
        MappingContext mappingContext = getMappingContext();
        setDBObjectValue(nativeEntry, key, value, mappingContext);
    }

    @Override
    protected String getPropertyKey(PersistentProperty prop) {
        if (prop instanceof Identity) {
            return MONGO_ID_FIELD;
        }
        return super.getPropertyKey(prop);
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

    public String getCollectionName(PersistentEntity persistentEntity) {
        return getCollectionName(persistentEntity, null);
    }

    private String getCollectionName(PersistentEntity persistentEntity, DBObject nativeEntry) {
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
    protected void setManyToMany(PersistentEntity persistentEntity, Object obj,
            DBObject nativeEntry, ManyToMany manyToMany, Collection associatedObjects,
            Map<Association, List<Serializable>> toManyKeys) {

        List ids = new ArrayList();
        if (associatedObjects != null) {
            for (Object o : associatedObjects) {
                if (o == null) {
                    ids.add(null);
                }
                else {
                    PersistentEntity childPersistentEntity =
                        getMappingContext().getPersistentEntity(o.getClass().getName());
                    EntityAccess entityAccess = createEntityAccess(childPersistentEntity, o);
                    ids.add(entityAccess.getIdentifier());
                }
            }
        }

        nativeEntry.put(manyToMany.getName() + "_$$manyToManyIds", ids);
    }

    @Override
    protected Collection getManyToManyKeys(PersistentEntity persistentEntity, Object object,
            Serializable nativeKey, DBObject nativeEntry, ManyToMany manyToMany) {
        return (Collection)nativeEntry.get(manyToMany.getName() + "_$$manyToManyIds");
    }

    protected Object getCurrentVersion(final EntityAccess ea) {
        Object currentVersion = ea.getProperty(GormProperties.VERSION);
        if (Number.class.isAssignableFrom(ea.getPropertyType(GormProperties.VERSION))) {
            currentVersion = currentVersion != null ? ((Number)currentVersion).longValue() : currentVersion;
        }
        return currentVersion;
    }

    @Override
    protected void deleteEntries(String family, final List<Object> keys) {
        getMongoTemplate().execute(new DbCallback<Object>() {
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

    @Override
    protected void cascadeDeleteCollection(EntityAccess entityAccess, Association association) {
        Object propValue = entityAccess.getProperty(association.getName());
        if (!(propValue instanceof Collection)) {
            return;
        }
        Collection collection = ((Collection) propValue);
        Persister persister = null;
        for (Iterator iter = collection.iterator(); iter.hasNext(); ) {
            Object child = iter.next();
            if (child == null) {
                log.warn("Encountered a null associated reference while cascade-deleting '{}' as part of {} (ID {})",
                        association.getReferencedPropertyName(), entityAccess.getEntity().getClass().getName(), entityAccess.getIdentifier());
                continue;
            }
            if(persister == null) {
                persister = session.getPersister(child);
            }
            persister.delete(child);
            iter.remove();
        }
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

    @Override
    public boolean isDirty(Object instance, Object entry) {
        if (super.isDirty(instance, entry)) {
            return true;
        }

        DBObject dbo = (DBObject)entry;
        PersistentEntity entity = getPersistentEntity();

        EntityAccess entityAccess = createEntityAccess(entity, instance, dbo);

        DBObject cached = (DBObject)((SessionImplementor<?>)getSession()).getCachedEntry(
                entity, (Serializable)entityAccess.getIdentifier(), true);

        return !dbo.equals(cached);
    }

    public MongoSession getMongoSession() {
        return (MongoSession) getSession();
    }

    private class MongoAssociationIndexer implements AssociationIndexer {
        private DBObject nativeEntry;
        private Association association;
        private MongoSession session;
        private boolean isReference = true;

        public MongoAssociationIndexer(DBObject nativeEntry, Association association, MongoSession session) {
            this.nativeEntry = nativeEntry;
            this.association = association;
            this.session = session;
            this.isReference = isReference(association);
        }

        public void preIndex(final Object primaryKey, final List foreignKeys) {
            // if the association is a unidirectional one-to-many we store the keys
            // embedded in the owning entity, otherwise we use a foreign key
            if (!association.isBidirectional()) {
                DB db = session.getNativeInterface();
                List dbRefs = new ArrayList();
                for (Object foreignKey : foreignKeys) {
                    if (isReference) {
                        dbRefs.add(new DBRef(getCollectionName(association.getAssociatedEntity()), foreignKey));
                    }
                    else {
                        dbRefs.add(foreignKey);
                    }
                }
                // update the native entry directly.
                nativeEntry.put(association.getName(), dbRefs);
            }
        }

        public void index(final Object primaryKey, final List foreignKeys) {
            // indexing is handled by putting the data in the native entry before it is persisted, see preIndex above.
        }

        public List query(Object primaryKey) {
            // for a unidirectional one-to-many we use the embedded keys
            if (!association.isBidirectional()) {
                final Object indexed = nativeEntry.get(association.getName());
                if (!(indexed instanceof Collection)) {
                    return Collections.emptyList();
                }
                List indexedList = getIndexedAssociationsAsList(indexed);

                if (associationsAreDbRefs(indexedList)) {
                    return extractIdsFromDbRefs(indexedList);
                }
                return indexedList;
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

        private List getIndexedAssociationsAsList(Object indexed) {
            return (indexed instanceof List) ? (List) indexed : new ArrayList(((Collection) indexed));
        }

        private boolean associationsAreDbRefs(List indexedList) {
            return !indexedList.isEmpty() && (indexedList.get(0) instanceof DBRef);
        }

        private List extractIdsFromDbRefs(List indexedList) {
            List resolvedDbRefs = new ArrayList();
            for (Object indexedAssociation : indexedList) {
                resolvedDbRefs.add(((DBRef) indexedAssociation).getId());
            }
            return resolvedDbRefs;
        }
    }
}
