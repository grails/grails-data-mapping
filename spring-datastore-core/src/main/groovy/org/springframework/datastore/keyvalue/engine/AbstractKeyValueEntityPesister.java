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
package org.springframework.datastore.keyvalue.engine;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.datastore.collection.PersistentList;
import org.springframework.datastore.collection.PersistentSet;
import org.springframework.datastore.core.Session;
import org.springframework.datastore.core.SessionImplementor;
import org.springframework.datastore.engine.*;
import org.springframework.datastore.keyvalue.mapping.Family;
import org.springframework.datastore.keyvalue.mapping.KeyValue;
import org.springframework.datastore.mapping.*;
import org.springframework.datastore.mapping.types.*;
import org.springframework.datastore.proxy.ProxyFactory;

import javax.persistence.CascadeType;
import javax.persistence.FetchType;
import java.io.Serializable;
import java.util.*;

/**
 * Abstract implementation of the EntityPersister abstract class
 * for key/value style stores
 * 
 * @author Graeme Rocher
 * @since 1.0
 */
public abstract class AbstractKeyValueEntityPesister<T,K> extends LockableEntityPersister {
    protected String entityFamily;
    protected ClassMapping classMapping;

    public AbstractKeyValueEntityPesister(MappingContext context, PersistentEntity entity, Session session) {
        super(context, entity, session);
        classMapping = entity.getMapping();
        entityFamily = getFamily(entity, classMapping);
    }


    public String getEntityFamily() {
        return entityFamily;
    }

    public ClassMapping getClassMapping() {
        return classMapping;
    }

    protected String getFamily(PersistentEntity persistentEntity, ClassMapping<Family> cm) {
        String table = null;
        if(cm.getMappedForm() != null) {
            table = cm.getMappedForm().getFamily();
        }
        if(table == null) table = persistentEntity.getJavaClass().getName();
        return table;
    }

    protected String getKeyspace(ClassMapping<Family> cm, String defaultValue) {
        String keyspace = null;
        if(cm.getMappedForm() != null) {
            keyspace = cm.getMappedForm().getKeyspace();
        }
        if(keyspace == null) keyspace = defaultValue;
        return keyspace;
    }


    @Override
    protected void deleteEntity(PersistentEntity persistentEntity, Object obj) {
        if(obj != null) {
            for (EntityInterceptor interceptor : interceptors) {
                if(!interceptor.beforeDelete(persistentEntity, createEntityAccess(persistentEntity, obj))) return;
            }

            final K key = readIdentifierFromObject(obj);

            SessionImplementor si = (SessionImplementor) session;
            if(key != null) {
                si.getPendingDeletes().add(new Runnable() {
                    public void run() {
                        deleteEntry(entityFamily, key);
                    }
                });


            }

        }
    }

    @Override
    protected EntityAccess createEntityAccess(PersistentEntity persistentEntity, Object obj) {
        return new EntityAccess(persistentEntity, obj);
    }

    protected EntityAccess createEntityAccess(PersistentEntity persistentEntity, Object obj, final T nativeEntry) {
        final NativeEntryModifyingEntityAccess ea = new NativeEntryModifyingEntityAccess(persistentEntity, obj);
        ea.setNativeEntry(nativeEntry);
        return ea;

    }

    protected class NativeEntryModifyingEntityAccess extends EntityAccess  {

        T nativeEntry;
        public NativeEntryModifyingEntityAccess(PersistentEntity persistentEntity, Object entity) {
            super(persistentEntity, entity);
        }

        @Override
        public void setProperty(String name, Object value) {
            super.setProperty(name, value);
            if(nativeEntry != null) {
                setEntryValue(nativeEntry, name, value);
            }
        }

        public void setNativeEntry(T nativeEntry) {
            this.nativeEntry = nativeEntry;
        }
    }

    /**
     * Deletes a single entry
     *
     * @param family The family
     * @param key The key
     */
    protected abstract void deleteEntry(String family, K key);

    @Override
    protected final void deleteEntities(PersistentEntity persistentEntity, Iterable objects) {
        if(objects != null) {
            final List<K> keys = new ArrayList<K>();
            for (Object object : objects) {
               K key = readIdentifierFromObject(object);
               if(key != null)
                    keys.add(key);
            }
            if(!keys.isEmpty()) {
                SessionImplementor si = (SessionImplementor) session;
                si.getPendingDeletes().add(new Runnable() {
                    public void run() {
                        deleteEntries(entityFamily, keys);
                    }
                });

            }
        }
    }

    private K readIdentifierFromObject(Object object) {
        EntityAccess access = createEntityAccess(getPersistentEntity(), object);
        access.setConversionService(getMappingContext().getConversionService());
        final Object idValue = access.getIdentifier();
        K key = null;
        if(idValue != null) {
            key = inferNativeKey(entityFamily, idValue);
        }
        return key;
    }

    public final Object lock(Serializable id) throws CannotAcquireLockException {
        return lock(id, DEFAULT_TIMEOUT);
    }

    public final Object lock(Serializable id, int timeout) throws CannotAcquireLockException {
        lockEntry(getPersistentEntity(), entityFamily, id, timeout);
        return retrieve(id);
    }

    /**
     * Subclasses can override to provide locking semantics
     *
     * @param persistentEntity The PesistentEntity instnace
     * @param entityFamily The family
     * @param id The identifer
     * @param timeout The lock timeout in seconds
     */
    protected void lockEntry(PersistentEntity persistentEntity, String entityFamily, Serializable id, int timeout) {
        // do nothing,
    }

    /**
     * Subclasses can override to provide locking semantics
     *
     * @param o The object
     * @return True if the object is locked
     */
    public boolean isLocked(Object o) {
        return false;
    }

    public void unlock(Object o) {
        unlockEntry(getPersistentEntity(), entityFamily, (Serializable) createEntityAccess(getPersistentEntity(), o).getIdentifier());
    }

    /**
     * Subclasses to override to provide locking semantics
     * @param persistentEntity The persistent entity
     * @param entityFamily The entity family
     * @param id The identifer
     */
    protected void unlockEntry(PersistentEntity persistentEntity, String entityFamily, Serializable id) {
        // do nothing
    }

    @Override
    protected final Object retrieveEntity(PersistentEntity persistentEntity, Serializable nativeKey) {

        final Serializable key = convertToNativeKey(nativeKey);
        T nativeEntry = retrieveEntry(persistentEntity, entityFamily, key);
        if(nativeEntry != null) {
            SessionImplementor<T> si = (SessionImplementor<T>) session;

            si.cacheEntry(persistentEntity,nativeKey, nativeEntry);

            return createObjectFromNativeEntry(persistentEntity, key, nativeEntry);
        }

        return null;
    }

    protected Serializable convertToNativeKey(Serializable nativeKey) {
        return nativeKey;
    }

    public Object proxy(Serializable key) {
        return getProxyFactory().createProxy(session, getPersistentEntity().getJavaClass(), key);
    }

    public Serializable refresh(Object o) {
        final PersistentEntity entity = getPersistentEntity();
        EntityAccess ea = createEntityAccess(entity, o);

        Serializable identifier = (Serializable) ea.getIdentifier();
        if(identifier != null) {
            final T entry = retrieveEntry(entity, entityFamily, identifier);
            refreshObjectStateFromNativeEntry(entity, o, identifier, entry);
            return identifier;
        }
        return null;
    }

    protected Object createObjectFromNativeEntry(PersistentEntity persistentEntity, Serializable nativeKey, T nativeEntry) {
        persistentEntity = discriminatePersistentEntity(persistentEntity, nativeEntry);
        Object obj = persistentEntity.newInstance();
        refreshObjectStateFromNativeEntry(persistentEntity, obj, nativeKey, nativeEntry);
        return obj;
    }

    protected void refreshObjectStateFromNativeEntry(PersistentEntity persistentEntity, Object obj, Serializable nativeKey, T nativeEntry) {
        EntityAccess ea = createEntityAccess(persistentEntity, obj, nativeEntry);
        ea.setConversionService(getMappingContext().getConversionService());
        String idName = ea.getIdentifierName();
        ea.setProperty(idName, nativeKey);

        final List<PersistentProperty> props = persistentEntity.getPersistentProperties();
        for (final PersistentProperty prop : props) {
            PropertyMapping<KeyValue> pm = prop.getMapping();
            String propKey = null;
            if(pm.getMappedForm()!=null) {
                propKey = pm.getMappedForm().getKey();
            }
            if(propKey == null) {
                propKey = prop.getName();
            }
            if(prop instanceof Simple) {
                ea.setProperty(prop.getName(), getEntryValue(nativeEntry, propKey) );
            }
            else if(prop instanceof ToOne) {
                Serializable tmp = (Serializable) getEntryValue(nativeEntry, propKey);
                PersistentEntity associatedEntity = prop.getOwner();
                final Serializable associationKey = (Serializable) getMappingContext().getConversionService().convert(tmp, associatedEntity.getIdentity().getType());
                if(associationKey != null) {

                    PropertyMapping<KeyValue> associationPropertyMapping = prop.getMapping();
                    boolean isLazy = isLazyAssociation(associationPropertyMapping);

                    final Class propType = prop.getType();
                    if(isLazy) {
                        Object proxy = getProxyFactory().createProxy(session, propType, associationKey);
                        ea.setProperty(prop.getName(), proxy);
                    }
                    else {
                        ea.setProperty(prop.getName(), session.retrieve(propType, associationKey));
                    }
                }
            }
            else if(prop instanceof OneToMany) {
                Association association = (Association) prop;
                PropertyMapping<KeyValue> associationPropertyMapping = association.getMapping();

                boolean isLazy = isLazyAssociation(associationPropertyMapping);
                AssociationIndexer indexer = getAssociationIndexer(association);
                nativeKey = (Serializable) getMappingContext().getConversionService().convert(nativeKey, getPersistentEntity().getIdentity().getType());
                if(isLazy) {
                    if(List.class.isAssignableFrom(association.getType())) {
                        ea.setPropertyNoConversion(association.getName(), new PersistentList(nativeKey, session, indexer));
                    }
                    else if(Set.class.isAssignableFrom(association.getType())) {
                        ea.setPropertyNoConversion(association.getName(), new PersistentSet(nativeKey, session, indexer));
                    }
                }
                else {
                    if(indexer != null) {
                        List keys = indexer.query(nativeKey);
                        ea.setProperty( association.getName(), session.retrieveAll(association.getAssociatedEntity().getJavaClass(), keys));
                    }
                }

            }
        }
    }



    /**
     * Subclasses should override to customize how entities in hierarchies are discriminated
     * @param persistentEntity The PersistentEntity
     * @param nativeEntry The native entry
     * @return The discriminated entity
     */
    protected PersistentEntity discriminatePersistentEntity(PersistentEntity persistentEntity, T nativeEntry) {
        return persistentEntity;
    }


    private boolean isLazyAssociation(PropertyMapping<KeyValue> associationPropertyMapping) {
        if(associationPropertyMapping != null) {
            KeyValue kv = associationPropertyMapping.getMappedForm();
            if(kv.getFetchStrategy() != FetchType.LAZY) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected final Serializable persistEntity(final PersistentEntity persistentEntity, Object obj) {
        ClassMapping<Family> cm = persistentEntity.getMapping();
        String family = entityFamily;

        T tmp = createNewEntry(family);
        final NativeEntryModifyingEntityAccess entityAccess = (NativeEntryModifyingEntityAccess) createEntityAccess(persistentEntity, obj, tmp );
        K k = readObjectIdentifier(entityAccess, cm);
        boolean isUpdate = k != null;

        final Serializable serializableKey = (Serializable) k;
        if(!isUpdate) {
            k = generateIdentifier(persistentEntity, tmp);
            String id = entityAccess.getIdentifierName();
            entityAccess.setProperty(id, k);            
        }
        else {
            SessionImplementor<T> si = (SessionImplementor<T>) session;


            tmp = si.getCachedEntry(persistentEntity, serializableKey);
            if(tmp == null) {
                tmp = retrieveEntry(persistentEntity, family, serializableKey);
            }
            entityAccess.setNativeEntry(tmp);
        }

        final T e = tmp;

        final List<PersistentProperty> props = persistentEntity.getPersistentProperties();
        final Map<OneToMany, List<Serializable>> oneToManyKeys = new HashMap<OneToMany, List<Serializable>>();
        final Map<OneToMany, Serializable> inverseCollectionUpdates = new HashMap<OneToMany, Serializable>();
        final Map<PersistentProperty, Object> toIndex = new HashMap<PersistentProperty, Object>();
        final Map<PersistentProperty, Object> toUnindex = new HashMap<PersistentProperty, Object>();
        for (PersistentProperty prop : props) {
            PropertyMapping<KeyValue> pm = prop.getMapping();
            final KeyValue keyValue = pm.getMappedForm();
            String key = null;
            if(keyValue != null) {
                key = keyValue.getKey();
            }
            final boolean indexed = keyValue != null && keyValue.isIndex();
            if(key == null) key = prop.getName();
            if(prop instanceof Simple) {
                Object propValue = entityAccess.getProperty(prop.getName());

                if(indexed) {
                    if(isUpdate) {
                        final Object oldValue = getEntryValue(e, key);
                        if(oldValue != null && !oldValue.equals(propValue))
                            toUnindex.put(prop, oldValue);
                    }

                    toIndex.put(prop, propValue);
                }
                setEntryValue(e, key, propValue);                

            }
            else if(prop instanceof OneToMany) {
                final OneToMany oneToMany = (OneToMany) prop;

                Object propValue = entityAccess.getProperty(oneToMany.getName());

                if(propValue instanceof Collection) {
                    Collection associatedObjects = (Collection) propValue;

                    List<Serializable> keys = session.persist(associatedObjects);

                    oneToManyKeys.put(oneToMany, keys);
                }

            }
            else if(prop instanceof ToOne) {
                ToOne association = (ToOne) prop;
                if(association.doesCascade(CascadeType.PERSIST)) {

                    if(!association.isForeignKeyInChild()) {

                        final Object associatedObject = entityAccess.getProperty(prop.getName());
                        if(associatedObject != null) {
                            ProxyFactory proxyFactory = getProxyFactory();
                            // never cascade to proxies
                            if(!proxyFactory.isProxy(associatedObject)) {
                                Serializable associationId;
                                AbstractKeyValueEntityPesister associationPersister = (AbstractKeyValueEntityPesister) session.getPersister(associatedObject);
                                if(!session.contains(associatedObject)) {
                                    Serializable tempId = associationPersister.getObjectIdentifier(associatedObject);
                                    if(tempId == null) tempId = session.persist(associatedObject);
                                    associationId = tempId;
                                }
                                else {
                                    associationId = associationPersister.getObjectIdentifier(associatedObject);
                                }

                                if(indexed) {
                                    toIndex.put(prop, associationId);
                                    if(isUpdate) {
                                        final Object oldValue = getEntryValue(e, key);
                                        if(oldValue != null && !oldValue.equals(associatedObject))
                                            toUnindex.put(prop, oldValue);
                                    }
                                }
                                setEntryValue(e, key, associationId);

                                if(association.isBidirectional()) {
                                    Association inverse = association.getInverseSide();
                                    if(inverse instanceof OneToMany) {
                                        inverseCollectionUpdates.put((OneToMany) inverse, associationId);
                                    }
                                }

                            }
                        }
                        else {
                            if(!association.isNullable() && !association.isCircular()) {
                                throw new DataIntegrityViolationException("Cannot save object ["+entityAccess.getEntity()+"] of type ["+persistentEntity+"]. The association ["+association+"] is cannot be null.");
                            }
                        }
                    }
                }

            }
        }


        if(!isUpdate) {

            final K updateId = k;
            SessionImplementor si = (SessionImplementor) session;
            si.getPendingInserts().add(new Runnable() {
                public void run() {
                    for (EntityInterceptor interceptor : interceptors) {
                            if(!interceptor.beforeInsert(persistentEntity, entityAccess)) return;
                    }
                    storeEntry(persistentEntity, updateId, e);
                    updateOneToManyIndices(updateId, oneToManyKeys);
                    toIndex.put(persistentEntity.getIdentity(), updateId);
                    updatePropertyIndices(updateId, toIndex, toUnindex);
                    for (OneToMany inverseCollection : inverseCollectionUpdates.keySet()) {
                        final Serializable primaryKey = inverseCollectionUpdates.get(inverseCollection);
                        final AbstractKeyValueEntityPesister inversePersister = (AbstractKeyValueEntityPesister) session.getPersister(inverseCollection.getOwner());
                        final AssociationIndexer associationIndexer = inversePersister.getAssociationIndexer(inverseCollection);
                        associationIndexer.index(primaryKey, updateId );
                    }
                }
            });
        }
        else {
            SessionImplementor si = (SessionImplementor) session;
            final K updateId = k;
            si.getPendingUpdates().add(new Runnable() {

                public void run() {
                    for (EntityInterceptor interceptor : interceptors) {
                            if(!interceptor.beforeUpdate(persistentEntity, entityAccess)) return;
                    }
                    updateEntry(persistentEntity, updateId, e);
                    updateOneToManyIndices(updateId, oneToManyKeys);
                    updatePropertyIndices(updateId, toIndex, toUnindex);
                }
            });
        }
        return serializableKey;
    }

    protected abstract K generateIdentifier(PersistentEntity persistentEntity, T id);

    private void updateOneToManyIndices(K identifier, Map<OneToMany, List<Serializable>> oneToManyKeys) {
        // now cascade onto one-to-many associations
        for (OneToMany oneToMany : oneToManyKeys.keySet()) {
            if(oneToMany.doesCascade(CascadeType.PERSIST)) {
                    final AssociationIndexer indexer = getAssociationIndexer(oneToMany);
                    if(indexer != null) {
                        indexer.index(identifier, oneToManyKeys.get(oneToMany));
                    }
            }
        }
    }

    private void updatePropertyIndices(K identifier, Map<PersistentProperty, Object> valuesToIndex, Map<PersistentProperty, Object> valuesToDeindex) {
        // Here we manually create indices for any indexed properties so that queries work
        for (PersistentProperty persistentProperty : valuesToIndex.keySet()) {
            Object value = valuesToIndex.get(persistentProperty);

            final PropertyValueIndexer indexer = getPropertyIndexer(persistentProperty);
            if(indexer != null) {
                indexer.index(value, identifier);
            }
        }

        for (PersistentProperty persistentProperty : valuesToDeindex.keySet()) {
            final PropertyValueIndexer indexer = getPropertyIndexer(persistentProperty);
            Object value = valuesToDeindex.get(persistentProperty);
            if(indexer != null) {
                indexer.deindex(value, identifier);
            }
        }

    }


    /**
     * Obtains an indexer for a particular property
     *
     * @param property The property to index
     * @return The indexer
     */
    public abstract PropertyValueIndexer getPropertyIndexer(PersistentProperty property);


    /**
     * Obtains an indexer for the given association
     *
     * @param association The association
     * @return An indexer
     */
    public abstract AssociationIndexer getAssociationIndexer(Association association);


    protected K readObjectIdentifier(EntityAccess entityAccess, ClassMapping<Family> cm) {
        return (K)entityAccess.getIdentifier();
    }


    protected String getIdentifierName(ClassMapping cm) {
        return cm.getIdentifier().getIdentifierName()[0];
    }

    /**
     * This is a rather simplistic and unoptimized implementation. Subclasses can override to provide
     * batch insert capabilities to optimize the insertion of multiple entities in one go
     * 
     * @param persistentEntity The persistent entity
     * @param objs The objext to persist
     * @return A list of keys
     */
    @Override
    protected List<Serializable> persistEntities(PersistentEntity persistentEntity, Iterable objs) {
        List<Serializable> keys = new ArrayList<Serializable>();
        for (Object obj : objs) {
            keys.add( persist(obj) );
        }
        return keys;
    }

    /**
     * Simplistic default implementation of retrieveAllEntities that iterates over each key and retrieves the entities 
     * one-by-one. Data stores that support batch retrieval can optimize this to retrieve all entities in one go.
     * 
     * @param persistentEntity The persist entity
     * @param keys The keys
     * @return A list of entities
     */
    @Override
    protected List<Object> retrieveAllEntities(PersistentEntity persistentEntity, Iterable<Serializable> keys) {
        List<Object> results = new ArrayList<Object>();
        for (Serializable key : keys) {
            results.add( retrieveEntity(persistentEntity, key));
        }
        return results;
    }

    /**
     * Simplistic default implementation of retrieveAllEntities that iterates over each key and retrieves the entities
     * one-by-one. Data stores that support batch retrieval can optimize this to retrieve all entities in one go.
     *
     * @param persistentEntity The persist entity
     * @param keys The keys
     * @return A list of entities
     */    
    @Override
    protected List<Object> retrieveAllEntities(PersistentEntity persistentEntity, Serializable[] keys) {
        List<Object> results = new ArrayList<Object>();
        for (Serializable key : keys) {
            results.add( retrieveEntity(persistentEntity, key));
        }
        return results;
    }

    /**
     * Used to establish the native key to use from the identifier defined by the object
     * @param family The family
     * @param identifier The identifier specified by the object
     * @return The native key which may just be a cast from the identifier parameter to K
     */
    protected K inferNativeKey(String family, Object identifier) {
        return (K) identifier;
    }

    /**
     * Creates a new entry for the given family.
     *
     * @param family The family
     * @return An entry such as a BigTable Entity, ColumnFamily etc.
     */
    protected abstract T createNewEntry(String family);

    /**
     * Reads a value for the given key from the native entry
     *
     * @param nativeEntry The native entry. Could be a ColumnFamily, a BigTable entity, a Map etc.
     * @param property The property key
     * @return The value
     */
    protected abstract Object getEntryValue(T nativeEntry, String property);

    /**
     * Sets a value on an entry
     * @param nativeEntry The native entry such as a BigTable Entity, ColumnFamily etc.
     * @param key The key
     * @param value The value
     */
    protected abstract void setEntryValue(T nativeEntry, String key, Object value);

    /**
     * Reads the native form of a Key/value datastore entry. This could be
     * a ColumnFamily, a BigTable Entity, a Map etc.
     *
     * @param persistentEntity The persistent entity
     * @param family The family
     * @param key The key
     * @return The native form
     */
    protected abstract T retrieveEntry(PersistentEntity persistentEntity, String family, Serializable key);

    /**
     * Stores the native form of a Key/value datastore to the actual data store
     *
     * @param persistentEntity The persistent entity
     * @param storeId
     *@param nativeEntry The native form. Could be a a ColumnFamily, BigTable Entity etc.
     *  @return The native key
     */
    protected abstract K storeEntry(PersistentEntity persistentEntity, K storeId, T nativeEntry);

    /**
     * Updates an existing entry to the actual datastore
     *
     * @param persistentEntity The PersistentEntity
     * @param key The key of the object to update
     * @param entry The entry
     */
    protected abstract void updateEntry(PersistentEntity persistentEntity, K key, T entry);

    /**
     * Deletes one or many entries for the given list of Keys
     *
     * @param family The family
     * @param keys The keys
     */
    protected abstract void deleteEntries(String family, List<K> keys);

}
