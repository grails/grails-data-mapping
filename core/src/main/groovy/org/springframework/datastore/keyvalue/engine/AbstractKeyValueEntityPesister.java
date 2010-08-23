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

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.core.convert.ConversionService;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.datastore.collection.PersistentList;
import org.springframework.datastore.collection.PersistentSet;
import org.springframework.datastore.core.Session;
import org.springframework.datastore.engine.*;
import org.springframework.datastore.keyvalue.convert.ByteArrayAwareTypeConverter;
import org.springframework.datastore.keyvalue.mapping.Family;
import org.springframework.datastore.keyvalue.mapping.KeyValue;
import org.springframework.datastore.mapping.*;
import org.springframework.datastore.mapping.types.*;
import org.springframework.datastore.reflect.ReflectionUtils;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract implementation of the EntityPersister abstract class
 * for key/value style stores
 * 
 * @author Graeme Rocher
 * @since 1.0
 */
public abstract class AbstractKeyValueEntityPesister<T,K> extends LockableEntityPersister {
    protected SimpleTypeConverter typeConverter;
    protected String entityFamily;
    protected ClassMapping classMapping;

    public AbstractKeyValueEntityPesister(MappingContext context, PersistentEntity entity, Session session) {
        super(context, entity, session);
        classMapping = entity.getMapping();
        entityFamily = getFamily(entity, classMapping);
        this.typeConverter = new ByteArrayAwareTypeConverter();

    }

    public void setConversionService(ConversionService conversionService) {
        typeConverter.setConversionService(conversionService);
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
                if(!interceptor.beforeDelete(persistentEntity, obj)) return;
            }

            K key = readIdentifierFromObject(obj);
            if(key != null) {
                deleteEntry(entityFamily, key);
            }
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
            List<K> keys = new ArrayList<K>();
            for (Object object : objects) {
               K key = readIdentifierFromObject(object);
               if(key != null)
                    keys.add(key);
            }
            if(!keys.isEmpty()) {
                deleteEntries(entityFamily, keys);
            }
        }
    }

    private K readIdentifierFromObject(Object object) {
        EntityAccess access = new EntityAccess(getPersistentEntity(), object);
        access.setConversionService(typeConverter.getConversionService());
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
        unlockEntry(getPersistentEntity(), entityFamily, (Serializable) new EntityAccess(getPersistentEntity(), o).getIdentifier());
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

        T nativeEntry = retrieveEntry(persistentEntity, entityFamily, nativeKey);
        if(nativeEntry != null) {
            return createObjectFromNativeEntry(persistentEntity, nativeKey, nativeEntry);
        }

        return null;
    }

    public Object proxy(Serializable key) {
        return getProxyInstance(getPersistentEntity().getJavaClass(), key);
    }

    private Object createObjectFromNativeEntry(PersistentEntity persistentEntity, Serializable nativeKey, T nativeEntry) {
        Object obj = persistentEntity.newInstance();

        EntityAccess ea = new EntityAccess(persistentEntity, obj);
        ea.setConversionService(typeConverter.getConversionService());
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
                final Serializable associationKey = (Serializable) typeConverter.convertIfNecessary(tmp, associatedEntity.getIdentity().getType());
                PropertyMapping<KeyValue> associationPropertyMapping = prop.getMapping();
                boolean isLazy = isLazyAssociation(associationPropertyMapping);

                final Class propType = prop.getType();
                if(isLazy) {
                    Class proxyClass = getProxyClass(propType);
                    Object proxy = createProxiedInstance(propType, proxyClass, associationKey);
                    ea.setProperty(prop.getName(), proxy);
                }
                else {
                    ea.setProperty(prop.getName(), session.retrieve(propType, associationKey));
                }
            }
            else if(prop instanceof OneToMany) {
                Association association = (Association) prop;
                PropertyMapping<KeyValue> associationPropertyMapping = association.getMapping();

                boolean isLazy = isLazyAssociation(associationPropertyMapping);
                AssociationIndexer indexer = getAssociationIndexer(association);
                if(isLazy) {
                    if(List.class.isAssignableFrom(association.getType())) {
                        ea.setProperty(association.getName(), new PersistentList(nativeKey, session, indexer));
                    }
                    else if(Set.class.isAssignableFrom(association.getType())) {
                        ea.setProperty(association.getName(), new PersistentSet(nativeKey, session, indexer));
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
        return obj;
    }

    private boolean isLazyAssociation(PropertyMapping<KeyValue> associationPropertyMapping) {
        if(associationPropertyMapping != null) {
            KeyValue kv = associationPropertyMapping.getMappedForm();
            if(kv.getFetchStrategy() != Fetch.LAZY) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected final Serializable persistEntity(PersistentEntity persistentEntity, EntityAccess entityAccess) {
        ClassMapping<Family> cm = persistentEntity.getMapping();
        String family = entityFamily;

        T e = createNewEntry(family);
        K k = readObjectIdentifier(entityAccess, cm);
        boolean isUpdate = k != null;

        for (EntityInterceptor interceptor : interceptors) {
            if(isUpdate) {
                if(!interceptor.beforeUpdate(persistentEntity, entityAccess.getEntity())) return (Serializable) k;
            }
            else {
                if(!interceptor.beforeInsert(persistentEntity, entityAccess.getEntity())) return null;
            }
        }

        final List<PersistentProperty> props = persistentEntity.getPersistentProperties();
        List<OneToMany> oneToManys = new ArrayList<OneToMany>();
        Map<PersistentProperty, Object> toIndex = new HashMap<PersistentProperty, Object>();
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
                final Object propValue = entityAccess.getProperty(prop.getName());
                setEntryValue(e, key, propValue);
                if(indexed) {
                    toIndex.put(prop, propValue);
                }

            }
            else if(prop instanceof OneToMany) {
                oneToManys.add((OneToMany) prop);
            }
            else if(prop instanceof ToOne) {
                ToOne association = (ToOne) prop;
                if(association.doesCascade(Cascade.SAVE)) {

                    if(!association.isForeignKeyInChild()) {

                        final Object associatedObject = entityAccess.getProperty(prop.getName());
                        if(associatedObject != null) {
                            Serializable associationId = session.persist(associatedObject);
                            setEntryValue(e, key, associationId);
                            if(indexed) {
                                toIndex.put(prop, associationId);
                            }

                        }
                        else {
                            throw new DataIntegrityViolationException("Cannot save object ["+entityAccess.getEntity()+"] of type ["+persistentEntity+"]. The association ["+association+"] is cannot be null.");
                        }
                    }
                }

            }
        }


        if(k == null) {
            k = storeEntry(persistentEntity, e);
            String id = entityAccess.getIdentifierName();
            entityAccess.setProperty(id, k);
        }
        else {
            updateEntry(persistentEntity, k, e);
        }

        // Here we manually create indices for any indexed properties so that queries work
        for (PersistentProperty persistentProperty : toIndex.keySet()) {
            Object value = toIndex.get(persistentProperty);

            final PropertyValueIndexer indexer = getPropertyIndexer(persistentProperty);
            if(indexer != null) {
                indexer.index(value, k);
            }
        }


        // now cascade onto one-to-many associations
        for (OneToMany oneToMany : oneToManys) {
            if(oneToMany.doesCascade(Cascade.SAVE)) {
                Object propValue = entityAccess.getProperty(oneToMany.getName());

                if(propValue instanceof Collection) {
                    Collection associatedObjects = (Collection) propValue;

                    List<Serializable> keys = session.persist(associatedObjects);

                    final AssociationIndexer indexer = getAssociationIndexer(oneToMany);
                    if(indexer != null) {
                        indexer.index(k, keys);
                    }
                }
            }

        }


        return (Serializable) k;
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
     * @param nativeEntry The native form. Could be a a ColumnFamily, BigTable Entity etc.
     *
     * @return The native key
     */
    protected abstract K storeEntry(PersistentEntity persistentEntity, T nativeEntry);

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
