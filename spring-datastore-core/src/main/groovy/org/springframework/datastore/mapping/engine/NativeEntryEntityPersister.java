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
package org.springframework.datastore.mapping.engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.FetchType;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.datastore.mapping.collection.PersistentList;
import org.springframework.datastore.mapping.collection.PersistentSet;
import org.springframework.datastore.mapping.config.Property;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.core.SessionImplementor;
import org.springframework.datastore.mapping.core.impl.PendingInsert;
import org.springframework.datastore.mapping.core.impl.PendingInsertAdapter;
import org.springframework.datastore.mapping.core.impl.PendingOperation;
import org.springframework.datastore.mapping.core.impl.PendingOperationAdapter;
import org.springframework.datastore.mapping.core.impl.PendingOperationExecution;
import org.springframework.datastore.mapping.core.impl.PendingUpdate;
import org.springframework.datastore.mapping.core.impl.PendingUpdateAdapter;
import org.springframework.datastore.mapping.model.ClassMapping;
import org.springframework.datastore.mapping.model.MappingContext;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.model.PersistentProperty;
import org.springframework.datastore.mapping.model.PropertyMapping;
import org.springframework.datastore.mapping.model.types.Association;
import org.springframework.datastore.mapping.model.types.Basic;
import org.springframework.datastore.mapping.model.types.Embedded;
import org.springframework.datastore.mapping.model.types.OneToMany;
import org.springframework.datastore.mapping.model.types.Simple;
import org.springframework.datastore.mapping.model.types.ToOne;
import org.springframework.datastore.mapping.proxy.ProxyFactory;

/**
 *
 * Provides an implementation of the {@link org.springframework.datastore.mapping.engine.EntityPersister} class that
 * reads and writes against a native datastore type specified by the generic type parameter T
 *
 * @author Graeme Rocher
 * @since  1.0
 */
public abstract class NativeEntryEntityPersister<T,K> extends LockableEntityPersister{
    protected ClassMapping classMapping;
    

    public NativeEntryEntityPersister(MappingContext mappingContext, PersistentEntity entity, Session session) {
        super(mappingContext, entity, session);
        classMapping = entity.getMapping();
    }

    public abstract String getEntityFamily();

    public ClassMapping getClassMapping() {
        return classMapping;
    }

    /**
     * Subclasses should override to optimize away manual property indexing if it is not required
     * 
     * @return True if property indexing is required (the default)
     */
    protected boolean doesRequirePropertyIndexing() { return true; }
    
    
    @Override
    protected void deleteEntity(PersistentEntity persistentEntity, Object obj) {
        if(obj != null) {
            for (EntityInterceptor interceptor : interceptors) {
                if(!interceptor.beforeDelete(persistentEntity, createEntityAccess(persistentEntity, obj))) return;
            }

            final K key = (K) readIdentifierFromObject(obj);

            if(key != null) {
                deleteEntry(getEntityFamily(), key);
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
                deleteEntries(getEntityFamily(), keys);
            }
        }
    }

    protected K readIdentifierFromObject(Object object) {
        EntityAccess access = createEntityAccess(getPersistentEntity(), object);
        access.setConversionService(getMappingContext().getConversionService());
        final Object idValue = access.getIdentifier();
        Object key = null;
        if(idValue != null) {
            key = inferNativeKey(getEntityFamily(), idValue);
        }
        return (K) key;
    }

    public final Object lock(Serializable id) throws CannotAcquireLockException {
        return lock(id, DEFAULT_TIMEOUT);
    }

    public final Object lock(Serializable id, int timeout) throws CannotAcquireLockException {
        lockEntry(getPersistentEntity(), getEntityFamily(), id, timeout);
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
        unlockEntry(getPersistentEntity(), getEntityFamily(), (Serializable) createEntityAccess(getPersistentEntity(), o).getIdentifier());
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
        T nativeEntry = retrieveEntry(persistentEntity, getEntityFamily(), key);
        if(nativeEntry != null) {
            SessionImplementor<Object> si = (SessionImplementor<Object>) session;

            si.cacheEntry(persistentEntity,nativeKey, nativeEntry);

            return createObjectFromNativeEntry(persistentEntity, key, nativeEntry);
        }

        return null;
    }

    /**
     * Subclasses should override to provide any conversion necessary to convert to a nativeKey
     * 
     * @param nativeKey The key
     * @return The native key
     */
    protected Serializable convertToNativeKey(Serializable nativeKey) {
        return nativeKey;
    }

    public Serializable refresh(Object o) {
        final PersistentEntity entity = getPersistentEntity();
        EntityAccess ea = createEntityAccess(entity, o);

        Serializable identifier = (Serializable) ea.getIdentifier();
        if(identifier != null) {
            final T entry = retrieveEntry(entity, getEntityFamily(), identifier);
            refreshObjectStateFromNativeEntry(entity, o, identifier, entry);
            return identifier;
        }
        return null;
    }

    public Object createObjectFromNativeEntry(PersistentEntity persistentEntity, Serializable nativeKey, T nativeEntry) {
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
            String propKey = getNativePropertyKey(prop);
            if((prop instanceof Simple) || (prop instanceof Basic)) {
                ea.setProperty(prop.getName(), getEntryValue(nativeEntry, propKey) );
            }
            else if(prop instanceof ToOne) {
            	if(prop instanceof Embedded) {
            		Embedded embedded = (Embedded) prop;
            		T embbeddedEntry = getEmbbeded(nativeEntry, propKey);
            		
            		if(embbeddedEntry != null) {
                		Object embeddedInstance = embedded.getAssociatedEntity().newInstance();
                		EntityAccess embeddedAccess = createEntityAccess(embedded.getAssociatedEntity(), embeddedInstance);
                		refreshObjectStateFromNativeEntry(embedded.getAssociatedEntity(), embeddedInstance, null, embbeddedEntry);
                		ea.setProperty(propKey, embeddedInstance);
            		}
            	}
            	else {
                    Serializable tmp = (Serializable) getEntryValue(nativeEntry, propKey);
                    if(tmp != null && !prop.getType().isInstance(tmp)) {
                        PersistentEntity associatedEntity = prop.getOwner();
                        final Serializable associationKey = (Serializable) getMappingContext().getConversionService().convert(tmp, associatedEntity.getIdentity().getType());
                        if(associationKey != null) {

                            PropertyMapping<Property> associationPropertyMapping = prop.getMapping();
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
            	}
            }
            else if(prop instanceof OneToMany) {
                Association association = (Association) prop;
                PropertyMapping<Property> associationPropertyMapping = association.getMapping();

                boolean isLazy = isLazyAssociation(associationPropertyMapping);
                AssociationIndexer indexer = getAssociationIndexer(nativeEntry, association);
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
     * Implementors should override to provide support for embedded objets
     * 
     * @param nativeEntry The native entry to read the embedded instance from
     * @param key The key
     * @return The native entry of the embedded instance
     */
    protected T getEmbbeded(T nativeEntry, String key) {
		return null;
	}

	protected String getNativePropertyKey(PersistentProperty prop) {
        PropertyMapping<Property> pm = prop.getMapping();
        String propKey = null;
        if(pm.getMappedForm()!=null) {
            propKey = pm.getMappedForm().getTargetName();
        }
        if(propKey == null) {
            propKey = prop.getName();
        }
        return propKey;
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

    private boolean isLazyAssociation(PropertyMapping<Property> associationPropertyMapping) {
        if(associationPropertyMapping != null) {
            Property kv = associationPropertyMapping.getMappedForm();
            if(kv.getFetchStrategy() != FetchType.LAZY) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
	@Override
    protected final Serializable persistEntity(final PersistentEntity persistentEntity, Object obj) {
        ClassMapping cm = persistentEntity.getMapping();
        String family = getEntityFamily();

        T tmp = null;
        final NativeEntryModifyingEntityAccess entityAccess = (NativeEntryModifyingEntityAccess) createEntityAccess(persistentEntity, obj, tmp );
        K k = readObjectIdentifier(entityAccess, cm);
        boolean isUpdate = k != null;
        
        PendingOperation<T, K> pendingOperation;

        SessionImplementor<Object> si = (SessionImplementor<Object>) session;
        if(!isUpdate) {
            tmp = createNewEntry(family);
            k = generateIdentifier(persistentEntity, tmp);
            
            pendingOperation = new PendingInsertAdapter<T, K>(persistentEntity, k, tmp, entityAccess) {
				@Override
				public void run() {
					executeInsert(persistentEntity, entityAccess, getNativeKey(), getNativeEntry());					
				}
			};
            
            String id = entityAccess.getIdentifierName();
            entityAccess.setProperty(id, k);
        }
        else {
            tmp = (T) si.getCachedEntry(persistentEntity, (Serializable) k);
            if(tmp == null) {
                tmp = retrieveEntry(persistentEntity, family, (Serializable) k);
            }
            if(tmp == null) {
                tmp = createNewEntry(family);
            }

            pendingOperation = new PendingUpdateAdapter<T, K>(persistentEntity, k, tmp, entityAccess) {
				@Override
				public void run() {
                    if(fireBeforeUpdate(persistentEntity, entityAccess)) return;
                    updateEntry(persistentEntity, getNativeKey(), getNativeEntry());

				}
			};            
        }

        final T e = tmp;
        entityAccess.setNativeEntry(e);

        final List<PersistentProperty> props = persistentEntity.getPersistentProperties();
        final Map<OneToMany, List<Serializable>> oneToManyKeys = new HashMap<OneToMany, List<Serializable>>();
        final Map<OneToMany, Serializable> inverseCollectionUpdates = new HashMap<OneToMany, Serializable>();
        final Map<PersistentProperty, Object> toIndex = new HashMap<PersistentProperty, Object>();
        final Map<PersistentProperty, Object> toUnindex = new HashMap<PersistentProperty, Object>();
        for (PersistentProperty prop : props) {
            PropertyMapping<Property> pm = prop.getMapping();
            final Property mappedProperty = pm.getMappedForm();
            String key = null;
            if(mappedProperty != null) {
                key = mappedProperty.getTargetName();
            }
            final boolean indexed = mappedProperty != null && mappedProperty.isIndex();
            if(key == null) key = prop.getName();
            if( (prop instanceof Simple) || (prop instanceof Basic)) {
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

                final Object propValue = entityAccess.getProperty(oneToMany.getName());

                if(propValue instanceof Collection) {
					Collection associatedObjects = (Collection) propValue;
                    List<Serializable> keys = session.persist(associatedObjects);
                    oneToManyKeys.put(oneToMany, keys);							                	
                }

            }
            else if(prop instanceof ToOne) {
                ToOne association = (ToOne) prop;
                if(prop instanceof Embedded) {
                	// For embedded properties simply set the entry value, the underlying implementation
                	// will have to store the embedded entity in an appropriate way (as a sub-document in a document store for example)
                	
                	Object embeddedInstance = entityAccess.getProperty(prop.getName());
                	if(embeddedInstance != null) {
                		NativeEntryEntityPersister<T,K> embeddedPersister = (NativeEntryEntityPersister<T,K>) session.getPersister(embeddedInstance);
                		T embeddedEntry = embeddedPersister.createNewEntry(embeddedPersister.getEntityFamily());
                		
                		final PersistentEntity associatedEntity = association.getAssociatedEntity();
                		final List<PersistentProperty> embeddedProperties = associatedEntity.getPersistentProperties();
                		final EntityAccess embeddedEntityAccess = createEntityAccess(associatedEntity, embeddedInstance);
                		for (PersistentProperty persistentProperty : embeddedProperties) {
							setEntryValue(embeddedEntry, persistentProperty.getName(), embeddedEntityAccess.getProperty(persistentProperty.getName()));
						}
                		
                		setEmbedded(e, key, embeddedEntry );
                	}
                }
                else if(association.doesCascade(CascadeType.PERSIST)) {

                    if(!association.isForeignKeyInChild()) {

                        final Object associatedObject = entityAccess.getProperty(prop.getName());
                        if(associatedObject != null) {
                            ProxyFactory proxyFactory = getProxyFactory();
                            // never cascade to proxies
                            if(!proxyFactory.isProxy(associatedObject)) {
                                Serializable associationId = null;
                                NativeEntryEntityPersister associationPersister = (NativeEntryEntityPersister) session.getPersister(associatedObject);
                                if(!session.contains(associatedObject)) {
                                    Serializable tempId = associationPersister.getObjectIdentifier(associatedObject);
                                    if(tempId == null) {
                                        if(association.isOwningSide())
                                            tempId = session.persist(associatedObject);
                                    }
                                    associationId = tempId;
                                }
                                else {
                                    associationId = associationPersister.getObjectIdentifier(associatedObject);
                                }

                                if(associationId != null) {
                                    if(indexed && doesRequirePropertyIndexing()) {
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
                                        else if(inverse instanceof ToOne) {
                                        	// TODO: Implement handling of bidirectional one-to-ones with foreign key in parent
                                        }
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
                    else {
                    	// TODO: Implement hasOne inverse association
                    }
                }

            }
        }


        if(!isUpdate) {
            // if the identifier is null at this point that means that datastore could not generated an identifer
            // and the identifer is generated only upon insert of the entity
        	boolean b = false;

            final K updateId = k;
            PendingOperation postOperation = new PendingOperationAdapter<T, K>(persistentEntity, k, e) {
				@Override
				public void run() {
                    updateOneToManyIndices(e, updateId, oneToManyKeys);
                    if(doesRequirePropertyIndexing()) {
                    	toIndex.put(persistentEntity.getIdentity(), updateId);
                    	updatePropertyIndices(updateId, toIndex, toUnindex);                    	
                    }
                    for (OneToMany inverseCollection : inverseCollectionUpdates.keySet()) {
                        final Serializable primaryKey = inverseCollectionUpdates.get(inverseCollection);
                        final NativeEntryEntityPersister inversePersister = (NativeEntryEntityPersister) session.getPersister(inverseCollection.getOwner());
                        final AssociationIndexer associationIndexer = inversePersister.getAssociationIndexer(e, inverseCollection);
                        associationIndexer.index(primaryKey, updateId );
                    }

				}
            	
            };
            pendingOperation.addCascadeOperation(postOperation);
            
            
            // If the key is still at this point we have to execute the pending operation now to get the key
            if(k == null) {
            	PendingOperationExecution.executePendingInsert(pendingOperation);
            }
            else {
            	si.addPendingInsert((PendingInsert) pendingOperation);
            }
        }
        else {
            final K updateId = k;
            
            PendingOperation postOperation = new PendingOperationAdapter<T, K>(persistentEntity, k, e) {
				@Override
				public void run() {
				      updateOneToManyIndices(e, updateId, oneToManyKeys);
				      if(doesRequirePropertyIndexing())
				    	  updatePropertyIndices(updateId, toIndex, toUnindex);	              
				}
            	
            };
            pendingOperation.addCascadeOperation(postOperation);
            si.addPendingUpdate((PendingUpdate) pendingOperation);
        }
        return (Serializable) k;
    }

    /**
     * Implementors should override this method to provide support for embedded objects
     * 
     * @param nativeEntry The native entry
     * @param key The key
     * @param value The embedded object
     */
    protected void setEmbedded(T nativeEntry, String key, T embeddedEntry) {
    	// do nothing. The default is no support for embedded instances
    }

	/**
     * Subclasses should override to provide id generation. If an identifier is only generated via an insert operation then this
     * method should return null
     * 
     * @param persistentEntity The entity
     * @param entry The native entry
     * @return The identifier or null if an identifier is generated only on insert
     */
    protected abstract K generateIdentifier(PersistentEntity persistentEntity, T entry);

    private void updateOneToManyIndices(T nativeEntry, Object identifier, Map<OneToMany, List<Serializable>> oneToManyKeys) {
        // now cascade onto one-to-many associations
        for (OneToMany oneToMany : oneToManyKeys.keySet()) {
            if(oneToMany.doesCascade(CascadeType.PERSIST)) {
                    final AssociationIndexer indexer = getAssociationIndexer(nativeEntry, oneToMany);
                    if(indexer != null) {
                        indexer.index(identifier, oneToManyKeys.get(oneToMany));
                    }
            }
        }
    }

    private void updatePropertyIndices(Object identifier, Map<PersistentProperty, Object> valuesToIndex, Map<PersistentProperty, Object> valuesToDeindex) {
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
     *
     * @param nativeEntry The native entry
     * @param association The association
     * @return An indexer
     */
    public abstract AssociationIndexer getAssociationIndexer(T nativeEntry, Association association);

    /**
     * Reads an objects identifier using the entity access and ClassMapping instance
     * @param entityAccess
     * @param cm
     * @return The object identifier
     */
    protected K readObjectIdentifier(EntityAccess entityAccess, ClassMapping cm) {
        return (K) entityAccess.getIdentifier();
    }

    /**
     * Obtains the identifier name to use. Subclasses can override to provide their own strategy for looking up an identifier name
     * @param cm The ClassMapping instance
     * @return The identifier name
     */
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
    protected Object inferNativeKey(String family, Object identifier) {
        return (Object) identifier;
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

    /**
     * Executes an insert for the given entity, entity access, identifier and native entry.
     * Any before interceptors will be triggered 
     * 
     * @param persistentEntity
     * @param entityAccess
     * @param id
     * @param e
     * @return The key
     */
    protected K executeInsert(final PersistentEntity persistentEntity,
                                   final NativeEntryModifyingEntityAccess entityAccess, final K id,
                                   final T e) {
		if(fireBeforeInsert(persistentEntity, entityAccess)) return null;
        final K newId = storeEntry(persistentEntity, id, e);
        entityAccess.setIdentifier(newId);
        return newId;
	}

    /**
     * Fire the beforeInsert even on an entityAccess object and return true if the operation should be evicted
     * @param persistentEntity The entity
     * @param entityAccess The entity access
     * @return True if the operation should be eviced
     */
	public boolean fireBeforeInsert(final PersistentEntity persistentEntity,
			final EntityAccess entityAccess) {
		for (EntityInterceptor interceptor : interceptors) {
		        if(!interceptor.beforeInsert(persistentEntity, entityAccess)) return true;
		}
		return false;
	}

    /**
     * Fire the beforeUpdate even on an entityAccess object and return true if the operation should be evicted
     * @param persistentEntity The entity
     * @param entityAccess The entity access
     * @return True if the operation should be eviced
     */	
    public boolean fireBeforeUpdate(final PersistentEntity persistentEntity,
			final EntityAccess entityAccess) {
		for (EntityInterceptor interceptor : interceptors) {
		    if(!interceptor.beforeUpdate(persistentEntity, entityAccess)) return true;
		}
		return false;
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
}
