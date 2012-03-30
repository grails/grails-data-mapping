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
package org.grails.datastore.mapping.engine;

import java.io.Serializable;
import java.util.*;

import javax.persistence.CascadeType;
import javax.persistence.FetchType;
import javax.persistence.FlushModeType;

import org.grails.datastore.mapping.cache.TPCacheAdapter;
import org.grails.datastore.mapping.cache.TPCacheAdapterRepository;
import org.grails.datastore.mapping.collection.*;
import org.grails.datastore.mapping.engine.internal.MappingUtils;
import org.grails.datastore.mapping.engine.types.CustomTypeMarshaller;
import org.grails.datastore.mapping.model.*;
import org.grails.datastore.mapping.model.types.*;
import org.grails.datastore.mapping.query.Query;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.CannotAcquireLockException;
import org.grails.datastore.mapping.config.Property;
import org.grails.datastore.mapping.core.Session;
import org.grails.datastore.mapping.core.SessionImplementor;
import org.grails.datastore.mapping.core.impl.PendingInsert;
import org.grails.datastore.mapping.core.impl.PendingInsertAdapter;
import org.grails.datastore.mapping.core.impl.PendingOperation;
import org.grails.datastore.mapping.core.impl.PendingOperationAdapter;
import org.grails.datastore.mapping.core.impl.PendingOperationExecution;
import org.grails.datastore.mapping.core.impl.PendingUpdate;
import org.grails.datastore.mapping.core.impl.PendingUpdateAdapter;
import org.grails.datastore.mapping.engine.event.PreDeleteEvent;
import org.grails.datastore.mapping.proxy.ProxyFactory;

/**
 * Provides an implementation of the {@link org.grails.datastore.mapping.engine.EntityPersister} class that
 * reads and writes against a native datastore type specified by the generic type parameter T
 *
 * @author Graeme Rocher
 * @since  1.0
 */
@SuppressWarnings({"unused", "rawtypes", "unchecked"})
public abstract class NativeEntryEntityPersister<T, K> extends LockableEntityPersister {
    protected ClassMapping classMapping;
    protected TPCacheAdapterRepository<T> cacheAdapterRepository;

    public NativeEntryEntityPersister(MappingContext mappingContext, PersistentEntity entity,
              Session session, ApplicationEventPublisher publisher) {
        super(mappingContext, entity, session, publisher);
        classMapping = entity.getMapping();
    }

    public NativeEntryEntityPersister(MappingContext mappingContext, PersistentEntity entity,
              Session session, ApplicationEventPublisher publisher, TPCacheAdapterRepository<T> cacheAdapterRepository) {
        super(mappingContext, entity, session, publisher);
        classMapping = entity.getMapping();
        this.cacheAdapterRepository = cacheAdapterRepository;
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
        if (obj == null) {
            return;
        }

        EntityAccess entityAccess = createEntityAccess(persistentEntity, obj);
        PreDeleteEvent event = new PreDeleteEvent(session.getDatastore(), persistentEntity, entityAccess);
        publisher.publishEvent(event);
        if (event.isCancelled()) {
            return;
        }

        final K key = readIdentifierFromObject(obj);
        if (key == null) {
            return;
        }

        FlushModeType flushMode = session.getFlushMode();
        try {
            session.setFlushMode(FlushModeType.COMMIT);
            cascadeBeforeDelete(persistentEntity, entityAccess, key, obj);
            deleteEntry(getEntityFamily(), key, obj);
            cascadeAfterDelete(persistentEntity, entityAccess, key, obj);
        }
        finally {
            session.setFlushMode(flushMode);
        }

        firePostDeleteEvent(persistentEntity, entityAccess);
    }

    protected void cascadeDeleteCollection(Collection collection) {
        for (Iterator iter = collection.iterator(); iter.hasNext(); ) {
            Object child = iter.next();
            deleteEntity(getMappingContext().getPersistentEntity(child.getClass().getName()), child);
            iter.remove();
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
     * @param entry the entry
     */
    protected abstract void deleteEntry(String family, K key, Object entry);

    /**
     * Delete collections before owner delete.
     */
    protected void cascadeBeforeDelete(PersistentEntity persistentEntity, EntityAccess entityAccess,
            K key, Object instance) {

        List<PersistentProperty> props = persistentEntity.getPersistentProperties();
        for (PersistentProperty prop : props) {
            String propertyKey = getPropertyKey(prop);

            if (prop instanceof OneToMany) {
                OneToMany oneToMany = (OneToMany)prop;
                if (oneToMany.isOwningSide() && oneToMany.doesCascade(CascadeType.REMOVE)) {
                    Object propValue = entityAccess.getProperty(oneToMany.getName());
                    if (propValue instanceof Collection) {
                        cascadeDeleteCollection((Collection) propValue);
                    }
                }
            }
            else if (prop instanceof ManyToMany) {
                ManyToMany manyToMany = (ManyToMany)prop;
                if (manyToMany.isOwningSide() && manyToMany.doesCascade(CascadeType.REMOVE)) {
                    Object propValue = entityAccess.getProperty(manyToMany.getName());
                    if (propValue instanceof Collection) {
                        cascadeDeleteCollection((Collection) propValue);
                    }
                }
            }
        }
    }

    /**
     * Delete many-to-ones after owner delete.
     */
    protected void cascadeAfterDelete(PersistentEntity persistentEntity, EntityAccess entityAccess,
            K key, Object instance) {

        List<PersistentProperty> props = persistentEntity.getPersistentProperties();
        for (PersistentProperty prop : props) {
            String propertyKey = getPropertyKey(prop);
            if (prop instanceof Basic) {
                Object propValue = entityAccess.getProperty(prop.getName());
            }
            else if (prop instanceof OneToMany) {
                OneToMany oneToMany = (OneToMany)prop;
                if (oneToMany.isOwningSide() && oneToMany.doesCascade(CascadeType.REMOVE)) {
                    Object propValue = entityAccess.getProperty(oneToMany.getName());
                    if (propValue instanceof Collection) {
                        cascadeDeleteCollection((Collection) propValue);
                    }
                }
            }
            else if (prop instanceof ToOne) {
                ToOne association = (ToOne) prop;
                if (!(prop instanceof Embedded) && !(prop instanceof EmbeddedCollection) &&
                        association.doesCascade(CascadeType.REMOVE)) {
                    if(association.isOwningSide()) {
                        Object value = entityAccess.getProperty(association.getName());
                        if(value != null) {
                            Persister persister = session.getPersister(value);
                            if(persister != null)
                                persister.delete(value);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected final void deleteEntities(PersistentEntity persistentEntity, Iterable objects) {
        if (objects != null) {
            final List<K> keys = new ArrayList<K>();
            for (Object object : objects) {
                K key = readIdentifierFromObject(object);
                if (key != null) {
                    keys.add(key);
                }
            }

            if (!keys.isEmpty()) {
                deleteEntries(getEntityFamily(), keys);
            }
        }
    }

    protected K readIdentifierFromObject(Object object) {
        EntityAccess access = createEntityAccess(getPersistentEntity(), object);
        access.setConversionService(getMappingContext().getConversionService());
        final Object idValue = access.getIdentifier();
        Object key = null;
        if (idValue != null) {
            key = inferNativeKey(getEntityFamily(), idValue);
        }
        return (K) key;
    }

    @Override
    public final Object lock(Serializable id) throws CannotAcquireLockException {
        return lock(id, DEFAULT_TIMEOUT);
    }

    @Override
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
    @Override
    public boolean isLocked(Object o) {
        return false;
    }

    @Override
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
        T nativeEntry = getFromTPCache(persistentEntity, nativeKey);
        if (nativeEntry == null) {
            nativeEntry = retrieveEntry(persistentEntity, getEntityFamily(), key);
            if (nativeEntry == null) {
                return null;
            }
        }

        return createObjectFromNativeEntry(persistentEntity, key, nativeEntry);
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
        if (identifier == null) {
            return null;
        }

        final T entry = retrieveEntry(entity, getEntityFamily(), identifier);
        refreshObjectStateFromNativeEntry(entity, o, identifier, entry, false);
        return identifier;
    }

    public Object createObjectFromNativeEntry(PersistentEntity persistentEntity, Serializable nativeKey, T nativeEntry) {
        persistentEntity = discriminatePersistentEntity(persistentEntity, nativeEntry);

        cacheNativeEntry(persistentEntity, nativeKey, nativeEntry);

        Object obj = newEntityInstance(persistentEntity);
        refreshObjectStateFromNativeEntry(persistentEntity, obj, nativeKey, nativeEntry, false);
        return obj;
    }

    protected void cacheNativeEntry(PersistentEntity persistentEntity,
            Serializable nativeKey, T nativeEntry) {
        SessionImplementor<Object> si = (SessionImplementor<Object>) session;
        Serializable key = (Serializable) getMappingContext().getConversionService().convert(
                nativeKey, persistentEntity.getIdentity().getType());
        si.cacheEntry(persistentEntity, key, nativeEntry);
    }

    protected void refreshObjectStateFromNativeEntry(PersistentEntity persistentEntity, Object obj,
                                                     Serializable nativeKey, T nativeEntry) {
        refreshObjectStateFromNativeEntry(persistentEntity, obj, nativeKey, nativeEntry, false);
    }
    protected void refreshObjectStateFromNativeEntry(PersistentEntity persistentEntity, Object obj,
                                                     Serializable nativeKey, T nativeEntry, boolean isEmbedded) {
        EntityAccess ea = createEntityAccess(persistentEntity, obj, nativeEntry);
        ea.setConversionService(getMappingContext().getConversionService());
        if(!(persistentEntity instanceof EmbeddedPersistentEntity)) {

            String idName = ea.getIdentifierName();
            ea.setProperty(idName, nativeKey);
        }

        final List<PersistentProperty> props = persistentEntity.getPersistentProperties();
        for (final PersistentProperty prop : props) {
            String propKey = getNativePropertyKey(prop);
            if (prop instanceof Simple) {
                ea.setProperty(prop.getName(), getEntryValue(nativeEntry, propKey));
            }
            else if(prop instanceof Basic) {
                Object entryValue = getEntryValue(nativeEntry, propKey);
                if(entryValue instanceof Map) {
                    entryValue = new LinkedHashMap((Map) entryValue);
                }
                else if(entryValue instanceof Collection) {
                    Collection collection = MappingUtils.createConcreteCollection(prop.getType());

                    Class propertyType = prop.getType();
                    Class genericType = MappingUtils.getGenericTypeForProperty(persistentEntity.getJavaClass(), prop.getName());
                    Collection collectionValue = (Collection) entryValue;
                    if(genericType != null && genericType.isEnum()) {
                        for (Object o : collectionValue) {
                            if(!(o instanceof Enum)) {
                                collection.add( Enum.valueOf(genericType, o.toString()) );
                            }
                        }
                    }
                    else {
                        collection.addAll(collectionValue);
                    }


                    entryValue = collection;
                }
                ea.setProperty(prop.getName(), entryValue);
            }
            else if (prop instanceof Custom) {
                handleCustom(prop, ea, nativeEntry);
            }
            else if (prop instanceof ToOne) {
                if (prop instanceof Embedded) {
                    Embedded embedded = (Embedded) prop;
                    T embeddedEntry = getEmbedded(nativeEntry, propKey);

                    if (embeddedEntry != null) {
                        Object embeddedInstance = newEntityInstance(embedded.getAssociatedEntity());
                        createEntityAccess(embedded.getAssociatedEntity(), embeddedInstance);
                        refreshObjectStateFromNativeEntry(embedded.getAssociatedEntity(),
                              embeddedInstance, null, embeddedEntry, true);
                        ea.setProperty(propKey, embeddedInstance);
                    }
                }

                else {
                    ToOne association = (ToOne) prop;

                    Serializable tmp = null;
                    if (!association.isForeignKeyInChild()) {
                        tmp = (Serializable) getEntryValue(nativeEntry, propKey);
                    }
                    else {
                        if (association.isBidirectional()) {

                            Query query = session.createQuery(association.getAssociatedEntity().getJavaClass());
                            query.eq(association.getInverseSide().getName(), obj)
                                  .projections().id();

                            tmp = (Serializable) query.singleResult();
                        }
                        else {
                            // TODO: handle unidirectional?
                        }


                    }
                    if (isEmbeddedEntry(tmp)) {
                        PersistentEntity associatedEntity = ((ToOne) prop).getAssociatedEntity();
                        Object instance = newEntityInstance(associatedEntity);
                        refreshObjectStateFromNativeEntry(associatedEntity,instance, null, (T) tmp, false);
                        ea.setProperty(prop.getName(), instance);
                    }
                    else if (tmp != null && !prop.getType().isInstance(tmp)) {
                        PersistentEntity associatedEntity = association.getAssociatedEntity();
                        final Serializable associationKey = (Serializable) getMappingContext().getConversionService().convert(
                              tmp, associatedEntity.getIdentity().getType());
                        if (associationKey != null) {

                            PropertyMapping<Property> associationPropertyMapping = prop.getMapping();
                            boolean isLazy = isLazyAssociation(associationPropertyMapping);

                            final Class propType = prop.getType();
                            if (isLazy) {
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
            else if (prop instanceof EmbeddedCollection) {
                    final Object embeddedInstances = getEntryValue(nativeEntry, propKey);
                    loadEmbeddedCollection((EmbeddedCollection)prop, ea, embeddedInstances, propKey);
            }
            else if (prop instanceof OneToMany) {
                Association association = (Association) prop;
                PropertyMapping<Property> associationPropertyMapping = association.getMapping();


                if(isEmbedded) {
                    List keys = loadEmbeddedCollectionKeys((Association) prop, ea, nativeEntry);
                    if (List.class.isAssignableFrom(association.getType())) {
                        ea.setPropertyNoConversion(association.getName(),
                                new PersistentList(keys, association.getAssociatedEntity().getJavaClass(), session));
                    }
                    else if (Set.class.isAssignableFrom(association.getType())) {
                        ea.setPropertyNoConversion(association.getName(),
                                new PersistentSet(keys, association.getAssociatedEntity().getJavaClass(), session));
                    }
                }
                else {
                    boolean isLazy = isLazyAssociation(associationPropertyMapping);
                    AssociationIndexer indexer = getAssociationIndexer(nativeEntry, association);
                    nativeKey = (Serializable) getMappingContext().getConversionService().convert(
                            nativeKey, getPersistentEntity().getIdentity().getType());
                    if (isLazy) {
                        if (List.class.isAssignableFrom(association.getType())) {
                            ea.setPropertyNoConversion(association.getName(),
                                    new PersistentList(nativeKey, session, indexer));
                        }
                        else if (SortedSet.class.isAssignableFrom(association.getType())) {
                            ea.setPropertyNoConversion(association.getName(),
                                    new PersistentSortedSet(nativeKey, session, indexer));
                        }
                        else if (Set.class.isAssignableFrom(association.getType())) {
                            ea.setPropertyNoConversion(association.getName(),
                                    new PersistentSet(nativeKey, session, indexer));
                        }
                    }
                    else {
                        if (indexer != null) {
                            List keys = indexer.query(nativeKey);
                            ea.setProperty(association.getName(),
                                    session.retrieveAll(association.getAssociatedEntity().getJavaClass(), keys));
                        }
                    }
                }
            }
            else if (prop instanceof ManyToMany) {
                ManyToMany manyToMany = (ManyToMany) prop;
                PropertyMapping<Property> associationPropertyMapping = manyToMany.getMapping();

                boolean isLazy = isLazyAssociation(associationPropertyMapping);
                nativeKey = (Serializable) getMappingContext().getConversionService().convert(
                        nativeKey, getPersistentEntity().getIdentity().getType());
                Class childType = manyToMany.getAssociatedEntity().getJavaClass();
                Collection cached = ((SessionImplementor)session).getCachedCollection(
                        persistentEntity, nativeKey, manyToMany.getName());
                if (cached == null) {
                    Collection collection;
                    if (isLazy) {
                        Collection keys = getManyToManyKeys(persistentEntity, obj, nativeKey,
                                nativeEntry, manyToMany);
                        if (List.class.isAssignableFrom(manyToMany.getType())) {
                            collection = new PersistentList(keys, childType, session);
                            ea.setPropertyNoConversion(manyToMany.getName(), collection);
                        }
                        else if (Set.class.isAssignableFrom(manyToMany.getType())) {
                            collection = new PersistentSet(keys, childType, session);
                            ea.setPropertyNoConversion(manyToMany.getName(), collection);
                        }
                        else {
                            collection = Collections.emptyList();
                        }
                    }
                    else {
                        AssociationIndexer indexer = getAssociationIndexer(nativeEntry, manyToMany);
                        if (indexer == null) {
                            if (List.class.isAssignableFrom(manyToMany.getType())) {
                                collection = Collections.emptyList();
                            }
                            else if (Set.class.isAssignableFrom(manyToMany.getType())) {
                                collection = Collections.emptySet();
                            }
                            else {
                                collection = Collections.emptyList();
                            }
                        }
                        else {
                            List keys = indexer.query(nativeKey);
                            collection = session.retrieveAll(childType, keys);
                            ea.setProperty(manyToMany.getName(), collection);
                        }
                    }
                    ((SessionImplementor)session).cacheCollection(
                            persistentEntity, nativeKey, collection, manyToMany.getName());
                }
                else {
                    ea.setProperty(manyToMany.getName(), cached);
                }
            }
        }
    }

    /**
     * Implementors who want to support one-to-many associations embedded should implement this method
     *
     * @param association The association
     * @param ea
     * @param nativeEntry
     *
     * @return A list of keys loaded from the embedded instance
     */
    protected List loadEmbeddedCollectionKeys(Association association, EntityAccess ea, T nativeEntry) {
        // no out of the box support
        return Collections.emptyList();
    }

    protected void setEmbeddedCollectionKeys(Association association, EntityAccess embeddedEntityAccess, T embeddedEntry, List<Serializable> keys) {
        // do nothing
    }


    /**
     * Tests whether a native entry is an embedded entry
     *
     * @param entry The native entry
     * @return True if it is embedded
     */
    protected boolean isEmbeddedEntry(Object entry) {
        return false;
    }

    /**
     * Implementors who want to the ability to read embedded collections should implement this method
     *
     * @param embeddedCollection The EmbeddedCollection instance
     * @param ea  The EntityAccess instance
     * @param embeddedInstances The embedded instances
     * @param propertyKey The property key
     */
    protected void loadEmbeddedCollection(EmbeddedCollection embeddedCollection, EntityAccess ea,
                                          Object embeddedInstances, String propertyKey) {
        // no support by default for embedded collections
    }

    /**
     * Implementors should override to provide support for embedded objects.
     *
     * @param nativeEntry The native entry to read the embedded instance from
     * @param key The key
     * @return The native entry of the embedded instance
     */
    protected T getEmbedded(T nativeEntry, String key) {
        return null;
    }

    private void handleCustom(PersistentProperty prop, EntityAccess ea, T nativeEntry) {
        CustomTypeMarshaller customTypeMarshaller = ((Custom) prop).getCustomTypeMarshaller();
        if (!customTypeMarshaller.supports(getSession().getDatastore())) {
            return;
        }

        Object value = customTypeMarshaller.read(prop, nativeEntry);
        ea.setProperty(prop.getName(), value);
    }

    protected Collection getManyToManyKeys(PersistentEntity persistentEntity, Object obj,
            Serializable nativeKey, T nativeEntry, ManyToMany manyToMany) {
        return null;
    }

    protected String getNativePropertyKey(PersistentProperty prop) {
        PropertyMapping<Property> pm = prop.getMapping();
        String propKey = null;
        if (pm.getMappedForm()!=null) {
            propKey = pm.getMappedForm().getTargetName();
        }
        if (propKey == null) {
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
        if (associationPropertyMapping == null) {
            return true;
        }

        Property kv = associationPropertyMapping.getMappedForm();
        return kv.getFetchStrategy() == FetchType.LAZY;
    }

    @Override
    protected final Serializable persistEntity(final PersistentEntity persistentEntity, Object obj) {
        T tmp = null;
        final NativeEntryModifyingEntityAccess entityAccess = (NativeEntryModifyingEntityAccess) createEntityAccess(persistentEntity, obj, tmp);

        K k = readObjectIdentifier(entityAccess, persistentEntity.getMapping());
        boolean isUpdate = k != null;
        boolean assignedId = false;
        if (isUpdate && !getSession().isDirty(obj)) {
            return (Serializable) k;
        }

        PendingOperation<T, K> pendingOperation;

        PropertyMapping mapping = persistentEntity.getIdentity().getMapping();
        if(mapping != null) {
            Property p = (Property) mapping.getMappedForm();
            assignedId = p != null && "assigned".equals(p.getGenerator());
            if(assignedId) {
                if(isUpdate && !session.contains(obj)) {
                    isUpdate = false;
                }
            }
        }
        String family = getEntityFamily();
        SessionImplementor<Object> si = (SessionImplementor<Object>) session;
        if (!isUpdate) {
            tmp = createNewEntry(family);

            if(!assignedId)
                k = generateIdentifier(persistentEntity, tmp);

            cacheNativeEntry(persistentEntity, (Serializable) k, tmp);

            pendingOperation = new PendingInsertAdapter<T, K>(persistentEntity, k, tmp, entityAccess) {
                public void run() {
                    executeInsert(persistentEntity, entityAccess, getNativeKey(), getNativeEntry());
                }
            };

            entityAccess.setProperty(entityAccess.getIdentifierName(), k);
        }
        else {
            tmp = (T) si.getCachedEntry(persistentEntity, (Serializable) k);
            if (tmp == null) {
                tmp = getFromTPCache(persistentEntity, (Serializable) k);
                if (tmp == null) {
                    tmp = retrieveEntry(persistentEntity, family, (Serializable) k);
                }
            }
            if (tmp == null) {
                tmp = createNewEntry(family);
            }

            final T finalTmp = tmp;
            final K finalK = k;
            pendingOperation = new PendingUpdateAdapter<T, K>(persistentEntity, finalK, finalTmp, entityAccess) {
                public void run() {
                    if (cancelUpdate(persistentEntity, entityAccess)) return;
                    updateEntry(persistentEntity, entityAccess, getNativeKey(), getNativeEntry());
                    updateTPCache(persistentEntity, finalTmp, (Serializable) finalK);
                    firePostUpdateEvent(persistentEntity, entityAccess);
                }
            };
        }

        final T e = tmp;
        entityAccess.setNativeEntry(e);

        final List<PersistentProperty> props = persistentEntity.getPersistentProperties();
        final Map<Association, List<Serializable>> toManyKeys = new HashMap<Association, List<Serializable>>();
        final Map<OneToMany, Serializable> inverseCollectionUpdates = new HashMap<OneToMany, Serializable>();
        final Map<PersistentProperty, Object> toIndex = new HashMap<PersistentProperty, Object>();
        final Map<PersistentProperty, Object> toUnindex = new HashMap<PersistentProperty, Object>();
        entityAccess.setToIndex(toIndex);
        for (PersistentProperty prop : props) {
            PropertyMapping<Property> pm = prop.getMapping();
            final Property mappedProperty = pm.getMappedForm();
            String key = null;
            if (mappedProperty != null) {
                key = mappedProperty.getTargetName();
            }
            if (key == null) key = prop.getName();
            final boolean indexed = isPropertyIndexed(mappedProperty);
            if ((prop instanceof Simple) || (prop instanceof Basic)) {
                Object propValue = entityAccess.getProperty(prop.getName());

                handleIndexing(isUpdate, e, toIndex, toUnindex, prop, key, indexed, propValue);
                setEntryValue(e, key, propValue);
            }
            else if ((prop instanceof Custom)) {
                CustomTypeMarshaller customTypeMarshaller = ((Custom) prop).getCustomTypeMarshaller();
                if (customTypeMarshaller.supports(getSession().getDatastore())) {
                    Object propValue = entityAccess.getProperty(prop.getName());
                    Object customValue = customTypeMarshaller.write(prop, propValue, e);
                    handleIndexing(isUpdate, e, toIndex, toUnindex, prop, key, indexed, customValue);
                }
            }
            else if (prop instanceof OneToMany) {
                final OneToMany oneToMany = (OneToMany) prop;

                final Object propValue = entityAccess.getProperty(oneToMany.getName());
                if (propValue instanceof Collection) {
                    Collection associatedObjects = (Collection) propValue;
                    if (isInitializedCollection(associatedObjects)) {
                        EntityPersister associationPersister = (EntityPersister) session.getPersister(oneToMany.getAssociatedEntity());
                        if(associationPersister != null) {
                            PersistentCollection persistentCollection;
                            boolean newCollection = false;
                            if(associatedObjects instanceof PersistentCollection) {
                                persistentCollection = (PersistentCollection) associatedObjects;
                            }
                            else {
                                Class associationType = oneToMany.getAssociatedEntity().getJavaClass();
                                persistentCollection = getPersistentCollection(associatedObjects, associationType);
                                entityAccess.setProperty(oneToMany.getName(), persistentCollection);
                                persistentCollection.markDirty();
                                newCollection = true;
                            }
                            if(persistentCollection.isDirty()) {
                                persistentCollection.resetDirty();
                                List<Serializable> keys = associationPersister.persist(associatedObjects);
                                toManyKeys.put(oneToMany, keys);
                                if(newCollection ) {
                                    entityAccess.setProperty(oneToMany.getName(), associatedObjects);
                                }
                            }
                        }
                    }

                }
            }
            else if (prop instanceof ManyToMany) {
                final ManyToMany manyToMany = (ManyToMany) prop;

                final Object propValue = entityAccess.getProperty(manyToMany.getName());
                if (propValue instanceof Collection) {
                    Collection associatedObjects = (Collection) propValue;
                    if(isInitializedCollection(associatedObjects)) {
                        setManyToMany(persistentEntity, obj, e, manyToMany, associatedObjects, toManyKeys);
                    }
                }
            }
            else if (prop instanceof ToOne) {
                ToOne association = (ToOne) prop;
                if (prop instanceof Embedded) {
                    // For embedded properties simply set the entry value, the underlying implementation
                    // will have to store the embedded entity in an appropriate way (as a sub-document in a document store for example)
                    handleEmbeddedToOne(association, key, entityAccess, e);
                }

                else if (association.doesCascade(CascadeType.PERSIST)) {
                    final Object associatedObject = entityAccess.getProperty(prop.getName());
                    if (associatedObject != null) {
                        @SuppressWarnings("hiding")
                        ProxyFactory proxyFactory = getProxyFactory();
                        // never cascade to proxies
                        if (proxyFactory.isInitialized(associatedObject)) {
                            Serializable associationId = null;
                            NativeEntryEntityPersister associationPersister = (NativeEntryEntityPersister) session.getPersister(associatedObject);
                            if (!session.contains(associatedObject)) {
                                Serializable tempId = associationPersister.getObjectIdentifier(associatedObject);
                                if (tempId == null) {
                                    if (association.isOwningSide()) {
                                        tempId = session.persist(associatedObject);
                                    }
                                }
                                associationId = tempId;
                            }
                            else {
                                associationId = associationPersister.getObjectIdentifier(associatedObject);
                            }

                            // handling of hasOne inverse key
                            if (association.isForeignKeyInChild()) {
                                T cachedAssociationEntry = (T) si.getCachedEntry(association.getAssociatedEntity(), associationId);
                                if (cachedAssociationEntry != null) {
                                    if (association.isBidirectional()) {
                                        Association inverseSide = association.getInverseSide();
                                        if (inverseSide != null) {
                                            setEntryValue(cachedAssociationEntry, inverseSide.getName(), formulateDatabaseReference(association.getAssociatedEntity(), inverseSide, (Serializable) k));
                                        }
                                        else {
                                            setEntryValue(cachedAssociationEntry, key,  formulateDatabaseReference(association.getAssociatedEntity(), inverseSide, (Serializable) k));
                                        }
                                    }
                                }

                                if(association.doesCascade(CascadeType.PERSIST)) {
                                    
                                    if(association.isBidirectional()) {
                                        Association inverseSide = association.getInverseSide();
                                        if(inverseSide != null) {
                                            EntityAccess inverseAccess = new EntityAccess(inverseSide.getOwner(), associatedObject);
                                            inverseAccess.setProperty(inverseSide.getName(), obj);
                                        }
                                    }
                                    associationPersister.persist(associatedObject);
                                }
                            }
                            // handle of standard many-to-one
                            else {
                                if (associationId != null) {
                                    if (indexed && doesRequirePropertyIndexing()) {
                                        toIndex.put(prop, associationId);
                                        if (isUpdate) {
                                            Object oldValue = getEntryValue(e, key);
                                            oldValue = oldValue != null ? convertToNativeKey( (Serializable) oldValue) : oldValue;

                                            if (oldValue != null && !oldValue.equals(associationId)) {
                                                toUnindex.put(prop, oldValue);
                                            }
                                        }
                                    }
                                    setEntryValue(e, key, formulateDatabaseReference(persistentEntity, association, associationId));

                                    if (association.isBidirectional()) {
                                        Association inverse = association.getInverseSide();
                                        if (inverse instanceof OneToMany) {
                                            inverseCollectionUpdates.put((OneToMany) inverse, associationId);
                                        }
                                        Object inverseEntity = entityAccess.getProperty(association.getName());
                                        if (inverseEntity != null) {
                                            EntityAccess inverseAccess = createEntityAccess(association.getAssociatedEntity(), inverseEntity);
                                            if (inverse instanceof OneToMany) {
                                                Collection existingValues = (Collection) inverseAccess.getProperty(inverse.getName());
                                                if (existingValues == null) {
                                                    existingValues = MappingUtils.createConcreteCollection(inverse.getType());
                                                    inverseAccess.setProperty(inverse.getName(), existingValues);
                                                }
                                                existingValues.add(entityAccess.getEntity());
                                            }
                                            else if (inverse instanceof ToOne) {
                                                inverseAccess.setProperty(inverse.getName(), entityAccess.getEntity());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else {
                       setEntryValue(e, getPropertyKey(prop), null);
                    }
                }
            }
            else if (prop instanceof EmbeddedCollection) {
                handleEmbeddedToMany(entityAccess, e, prop, key);
            }
        }

        if (!isUpdate) {
            // if the identifier is null at this point that means that datastore could not generated an identifer
            // and the identifer is generated only upon insert of the entity

            final K updateId = k;
            PendingOperation postOperation = new PendingOperationAdapter<T, K>(persistentEntity, k, e) {
                public void run() {
                    updateToManyIndices(e, updateId, toManyKeys);

                    if (doesRequirePropertyIndexing()) {
                        toIndex.put(persistentEntity.getIdentity(), updateId);
                        updatePropertyIndices(updateId, toIndex, toUnindex);
                    }
                    for (OneToMany inverseCollection : inverseCollectionUpdates.keySet()) {
                        final Serializable primaryKey = inverseCollectionUpdates.get(inverseCollection);
                        final NativeEntryEntityPersister inversePersister = (NativeEntryEntityPersister) session.getPersister(inverseCollection.getOwner());
                        final AssociationIndexer associationIndexer = inversePersister.getAssociationIndexer(e, inverseCollection);
                        associationIndexer.index(primaryKey, updateId);
                    }
                }
            };
            pendingOperation.addCascadeOperation(postOperation);

            // If the key is still null at this point we have to execute the pending operation now to get the key
            if (k == null) {
                PendingOperationExecution.executePendingOperation(pendingOperation);
            }
            else {
                si.addPendingInsert((PendingInsert) pendingOperation);
            }
        }
        else {
            final K updateId = k;

            PendingOperation postOperation = new PendingOperationAdapter<T, K>(persistentEntity, k, e) {
                public void run() {
                    updateToManyIndices(e, updateId, toManyKeys);
                    if (doesRequirePropertyIndexing()) {
                        updatePropertyIndices(updateId, toIndex, toUnindex);
                    }
                }
            };
            pendingOperation.addCascadeOperation(postOperation);
            si.addPendingUpdate((PendingUpdate) pendingOperation);
        }
        return (Serializable) k;
    }

    private AbstractPersistentCollection getPersistentCollection(Collection associatedObjects, Class associationType) {
        if(associatedObjects instanceof Set) {
            return associatedObjects instanceof SortedSet ? new PersistentSortedSet(associationType,getSession(), (SortedSet) associatedObjects) : new PersistentSet(associationType, getSession(), associatedObjects);
        }
        else {
            return new PersistentList(associationType,getSession(), (List) associatedObjects);
        }
    }

    private boolean isInitializedCollection(Collection associatedObjects) {
        return !(associatedObjects instanceof PersistentCollection) || ((PersistentCollection) associatedObjects).isInitialized();
    }

    /**
     * Formulates a database reference for the given entity, association and association id
     *
     * @param persistentEntity The entity being persisted
     * @param association The association
     * @param associationId The association id
     * @return A database reference
     */
    protected Object formulateDatabaseReference(PersistentEntity persistentEntity, Association association, Serializable associationId) {
        return associationId;
    }

    protected void handleEmbeddedToMany(EntityAccess entityAccess, T e, PersistentProperty prop, String key) {
        // For embedded properties simply set the entry value, the underlying implementation
        // will have to store the embedded entity in an appropriate way (as a sub-document in a document store for example)
        Object embeddedInstances = entityAccess.getProperty(prop.getName());
        if (!(embeddedInstances instanceof Collection) || ((Collection)embeddedInstances).isEmpty()) {
            return;
        }

        Collection instances = (Collection)embeddedInstances;
        List<T> embeddedEntries = new ArrayList<T>();
        for (Object instance : instances) {
            T entry = handleEmbeddedInstance((Association) prop, instance);
            embeddedEntries.add(entry);
        }

        setEmbeddedCollection(e, key, instances, embeddedEntries);
    }

    protected void handleEmbeddedToOne(Association association, String key, EntityAccess entityAccess, T nativeEntry) {
        Object embeddedInstance = entityAccess.getProperty(association.getName());
        if (embeddedInstance == null) {
            return;
        }

        T embeddedEntry = handleEmbeddedInstance(association, embeddedInstance);
        setEmbedded(nativeEntry, key, embeddedEntry);
    }

    protected T handleEmbeddedInstance(Association association, Object embeddedInstance) {
        NativeEntryEntityPersister<T,K> embeddedPersister = (NativeEntryEntityPersister<T,K>) session.getPersister(embeddedInstance);

        // embeddedPersister would be null if the associated entity is a EmbeddedPersistentEntity
        T embeddedEntry;
        if(embeddedPersister == null) {
            embeddedEntry = createNewEntry(association.getName());
        }
        else {
            embeddedEntry = embeddedPersister.createNewEntry(embeddedPersister.getEntityFamily());
        }

        final PersistentEntity associatedEntity = association.getAssociatedEntity();
        if (associatedEntity != null) {
            final List<PersistentProperty> embeddedProperties = associatedEntity.getPersistentProperties();
            final EntityAccess embeddedEntityAccess = createEntityAccess(associatedEntity, embeddedInstance);
            for (PersistentProperty persistentProperty : embeddedProperties) {
                if (persistentProperty instanceof Simple) {
                    setEntryValue(embeddedEntry, getPropertyKey(persistentProperty), embeddedEntityAccess.getProperty(persistentProperty.getName()));
                }
                else if (persistentProperty instanceof Custom) {
                    Custom custom = (Custom)persistentProperty;
                    handleCustom(custom, embeddedEntityAccess, embeddedEntry);
                }
                else if (persistentProperty instanceof Association) {
                    if(persistentProperty instanceof Embedded) {
                        Association toOne = (Association) persistentProperty;

                        handleEmbeddedToOne(toOne,getPropertyKey(persistentProperty) , embeddedEntityAccess, embeddedEntry);
                    }
                    else if (persistentProperty instanceof ToOne) {
                        Association toOne = (Association) persistentProperty;

                        Object obj = embeddedEntityAccess.getProperty(toOne.getName());
                        Persister persister = getSession().getPersister(obj);
                        if(persister != null) {
                            Serializable id = persister.persist(obj);
                            if(id != null) {
                                setEntryValue(embeddedEntry, getPropertyKey(toOne), formulateDatabaseReference(embeddedPersister.getPersistentEntity(), association, id));
                            }
                        }

                    }
                    else if(persistentProperty instanceof Basic) {
                        setEntryValue(embeddedEntry, getPropertyKey(persistentProperty), embeddedEntityAccess.getProperty(persistentProperty.getName()));
                    }
                    else if(persistentProperty instanceof EmbeddedCollection) {
                        handleEmbeddedToMany(embeddedEntityAccess, embeddedEntry, persistentProperty, persistentProperty.getName());
                    }
                    else {
                        if (persistentProperty instanceof OneToMany) {
                            final OneToMany oneToMany = (OneToMany) persistentProperty;

                            final Object propValue = embeddedEntityAccess.getProperty(oneToMany.getName());
                            if (propValue instanceof Collection) {
                                Collection associatedObjects = (Collection) propValue;
                                List<Serializable> keys = session.persist(associatedObjects);

                                setEmbeddedCollectionKeys(oneToMany, embeddedEntityAccess, embeddedEntry, keys);
                            }
                        }
                        else if (persistentProperty instanceof ManyToMany) {
                            final ManyToMany manyToMany = (ManyToMany) persistentProperty;

                            final Object propValue = embeddedEntityAccess.getProperty(manyToMany.getName());
                            if (propValue instanceof Collection) {
                                Collection associatedObjects = (Collection) propValue;
                                List<Serializable> keys = session.persist(associatedObjects);
                                setManyToMany(embeddedPersister.getPersistentEntity(), embeddedInstance, embeddedEntry, manyToMany, associatedObjects, Collections.<Association, List<Serializable>>emptyMap());
                            }
                        }
                    }
                }
            }
        }
        return embeddedEntry;
    }


    private void handleIndexing(boolean update, T e, Map<PersistentProperty, Object> toIndex,
            Map<PersistentProperty, Object> toUnindex, PersistentProperty prop, String key,
            boolean indexed, Object propValue) {

        if (!indexed) {
            return;
        }

        if (update) {
            final Object oldValue = getEntryValue(e, key);
            if (oldValue != null && !oldValue.equals(propValue)) {
                toUnindex.put(prop, oldValue);
            }
        }

        toIndex.put(prop, propValue);
    }

    protected boolean isPropertyIndexed(Property mappedProperty) {
        return mappedProperty != null && mappedProperty.isIndex();
    }

    protected void setManyToMany(PersistentEntity persistentEntity, Object obj,
            T nativeEntry, ManyToMany manyToMany, Collection associatedObjects,
            Map<Association, List<Serializable>> toManyKeys) {
        // override as necessary
    }

    /**
     * Implementors should override this method to provide support for embedded objects
     *
     * @param nativeEntry The native entry
     * @param key The key
     * @param embeddedEntry The embedded object
     */
    protected void setEmbedded(T nativeEntry, String key, T embeddedEntry) {
        // do nothing. The default is no support for embedded instances
    }

    /**
     * Implementors should override this method to provide support for embedded objects
     *
     * @param nativeEntry The native entry
     * @param key The key
     * @param instances the embedded instances
     * @param embeddedEntries the native entries
     */
    protected void setEmbeddedCollection(T nativeEntry, String key, Collection<?> instances, List<T> embeddedEntries) {
        // do nothing. The default is no support for embedded collections
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

    private void updateToManyIndices(T nativeEntry, Object identifier, Map<Association, List<Serializable>> toManyKeys) {
        // now cascade onto one-to-many associations
        for (Association association : toManyKeys.keySet()) {
            if (association.doesCascade(CascadeType.PERSIST)) {
                final AssociationIndexer indexer = getAssociationIndexer(nativeEntry, association);
                if (indexer != null) {
                    indexer.index(identifier, toManyKeys.get(association));
                }
            }
        }
    }

    private void updatePropertyIndices(Object identifier, Map<PersistentProperty, Object> valuesToIndex, Map<PersistentProperty, Object> valuesToDeindex) {
        // Here we manually create indices for any indexed properties so that queries work
        for (PersistentProperty persistentProperty : valuesToIndex.keySet()) {
            Object value = valuesToIndex.get(persistentProperty);

            final PropertyValueIndexer indexer = getPropertyIndexer(persistentProperty);
            if (indexer != null) {
                indexer.index(value, identifier);
            }
        }

        for (PersistentProperty persistentProperty : valuesToDeindex.keySet()) {
            final PropertyValueIndexer indexer = getPropertyIndexer(persistentProperty);
            Object value = valuesToDeindex.get(persistentProperty);
            if (indexer != null) {
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
        Iterable newIter = objs;
        if(objs instanceof Collection) {
            newIter = new ArrayList((Collection) objs);
        }
        for (Object obj : newIter) {
            keys.add(persist(obj));
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
            results.add(retrieveEntity(persistentEntity, key));
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
            results.add(retrieveEntity(persistentEntity, key));
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
        return identifier;
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
     * @param entityAccess The EntityAccess
     * @param storeId
     * @param nativeEntry The native form. Could be a a ColumnFamily, BigTable Entity etc.
     * @return The native key
     */
    protected abstract K storeEntry(PersistentEntity persistentEntity, EntityAccess entityAccess,
                                    K storeId, T nativeEntry);

    /**
     * Updates an existing entry to the actual datastore
     *
     * @param persistentEntity The PersistentEntity
     * @param entityAccess The EntityAccess
     * @param key The key of the object to update
     * @param entry The entry
     */
    protected abstract void updateEntry(PersistentEntity persistentEntity,
            EntityAccess entityAccess, K key, T entry);

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
                              final NativeEntryModifyingEntityAccess entityAccess,
                              final K id, final T e) {
        if (cancelInsert(persistentEntity, entityAccess)) return null;
        final K newId = storeEntry(persistentEntity, entityAccess, id, e);
        entityAccess.setIdentifier(newId);
        updateTPCache(persistentEntity, e, (Serializable) newId);

        firePostInsertEvent(persistentEntity, entityAccess);
        return newId;
    }

    protected void updateTPCache(PersistentEntity persistentEntity, T e, Serializable id) {
        if (cacheAdapterRepository == null) {
            return;
        }

        TPCacheAdapter<T> cacheAdapter = cacheAdapterRepository.getTPCacheAdapter(persistentEntity);
        if (cacheAdapter != null) {
            cacheAdapter.cacheEntry(id, e);
        }
    }

    protected T getFromTPCache(PersistentEntity persistentEntity, Serializable id) {
        if (cacheAdapterRepository == null) {
            return null;
        }

        TPCacheAdapter<T> cacheAdapter = cacheAdapterRepository.getTPCacheAdapter(persistentEntity);
        if (cacheAdapter != null) {
            return cacheAdapter.getCachedEntry(id);
        }
        return null;
    }

    protected class NativeEntryModifyingEntityAccess extends EntityAccess {

        T nativeEntry;
        private Map<PersistentProperty, Object> toIndex;

        public NativeEntryModifyingEntityAccess(PersistentEntity persistentEntity, Object entity) {
            super(persistentEntity, entity);
        }

        @Override
        public void setProperty(String name, Object value) {
            super.setProperty(name, value);
            if (nativeEntry != null) {
                PersistentProperty property = persistentEntity.getPropertyByName(name);
                if (property != null && (property instanceof Simple)) {
                    setEntryValue(nativeEntry, name, value);
                }

                if(toIndex != null && property != null) {
                    PropertyMapping<Property> pm = property.getMapping();
                    if(pm != null && isPropertyIndexed(pm.getMappedForm())) {
                        if(property instanceof ToOne) {
                            ToOne association = (ToOne) property;
                            if(!association.isForeignKeyInChild()) {
                                NativeEntryEntityPersister associationPersister = (NativeEntryEntityPersister) session.getPersister(value);
                                toIndex.put(property, associationPersister.getObjectIdentifier(value));
                            }
                            
                        }
                        else {
                            toIndex.put(property, value);
                        }
                    }
                }
            }
        }

        public void setNativeEntry(T nativeEntry) {
            this.nativeEntry = nativeEntry;
        }

        public void setToIndex(Map<PersistentProperty, Object> toIndex) {
            this.toIndex = toIndex;
        }
    }

    public boolean isDirty(Object instance, Object entry) {
        if ((instance == null)) {
            return false;
        }
        else if(entry == null) {
            return true;
        }

        T nativeEntry;
        try {
            nativeEntry = (T)entry;
        }
        catch (ClassCastException ignored) {
            return false;
        }

        EntityAccess entityAccess = createEntityAccess(getPersistentEntity(), instance, nativeEntry);

        List<PersistentProperty> props = getPersistentEntity().getPersistentProperties();
        for (PersistentProperty prop : props) {
            String key = getPropertyKey(prop);

            Object currentValue = entityAccess.getProperty(prop.getName());
            Object oldValue = getEntryValue(nativeEntry, key);
            if (prop instanceof Simple || prop instanceof Basic || prop instanceof ToOne ) {
                if (!areEqual(oldValue, currentValue, key)) {
                    return true;
                }
            }
            else if (prop instanceof OneToMany || prop instanceof ManyToMany) {
                if (!areCollectionsEqual(oldValue, currentValue)) {
                    return true;
                }
            }
            else if (prop instanceof EmbeddedCollection) {
                if(currentValue != null && oldValue == null) return true;
                if((currentValue instanceof Collection) && (oldValue instanceof Collection)) {
                    Collection currentCollection = (Collection) currentValue;
                    Collection oldCollection = (Collection) oldValue;
                    if(currentCollection.size() != oldCollection.size()) {
                        return true;
                    }
                    else {
                        if(!areCollectionsEqual(oldValue, currentValue)) {
                            return true;
                        }
                    }
                }

            }
            else if (prop instanceof Custom) {
                CustomTypeMarshaller marshaller = ((Custom)prop).getCustomTypeMarshaller();
                return !areEqual(marshaller.read(prop, entry), currentValue, key);
            }
            else {
                throw new UnsupportedOperationException("dirty not detected for property " + prop.toString() + " " + prop.getClass().getSuperclass().toString());
            }
        }

        return false;
    }

    protected String getPropertyKey(PersistentProperty prop) {
        PropertyMapping<Property> pm = prop.getMapping();
        Property mappedProperty = pm.getMappedForm();
        String key = null;
        if (mappedProperty != null) {
            key = mappedProperty.getTargetName();
        }
        if (key == null) key = prop.getName();
        return key;
    }

    protected boolean areCollectionsEqual(Object oldValue, Object currentValue) {
        if (oldValue == currentValue) {
            // same or both null
            return true;
        }

        if (currentValue instanceof PersistentCollection) {
            return !((PersistentCollection)currentValue).isDirty();
        }



        return replaceNullOrUninitialized(oldValue, currentValue).equals(
                replaceNullOrUninitialized(currentValue, oldValue));
    }

    private Object replaceNullOrUninitialized(Object c, Object other) {
        if (c == null) {
            if (other instanceof Set) {
                return Collections.emptySet();
            }
            return Collections.emptyList();
        }

        if (c instanceof PersistentCollection && !((PersistentCollection)c).isInitialized()) {
            if (c instanceof Set) {
                return Collections.emptySet();
            }
            return Collections.emptyList();
        }

        return c;
    }

    protected boolean areEqual(Object oldValue, Object currentValue, String propName) {
        if (oldValue == currentValue) {
            return true;
        }

        if (oldValue == null || currentValue == null) {
            return false;
        }

        if ("version".equals(propName)) {
            // special case where comparing int and long would fail artifically
            if (oldValue instanceof Number && currentValue instanceof Number) {
                oldValue = ((Number)oldValue).longValue();
                currentValue = ((Number)currentValue).longValue();
            }
            else {
                oldValue = oldValue.toString();
                currentValue = currentValue.toString();
            }
        }

        Class oldValueClass = oldValue.getClass();
        if (!oldValueClass.isArray()) {
            if (oldValue instanceof Float) {
                return Float.floatToIntBits((Float)oldValue) == Float.floatToIntBits((Float)currentValue);
            }
            if (oldValue instanceof Double) {
                return Double.doubleToLongBits((Double)oldValue) == Double.doubleToLongBits((Double)currentValue);
            }
            return oldValue.equals(currentValue);
        }

        // check arrays

        if (oldValue.getClass() != currentValue.getClass()) {
            // different dimension
            return false;
        }

        if (oldValue instanceof long[]) {
            return Arrays.equals((long[])oldValue, (long[])currentValue);
        }

        if (oldValue instanceof int[]) {
            return Arrays.equals((int[])oldValue, (int[])currentValue);
        }

        if (oldValue instanceof short[]) {
            return Arrays.equals((short[])oldValue, (short[])currentValue);
        }

        if (oldValue instanceof char[]) {
            return Arrays.equals((char[])oldValue, (char[])currentValue);
        }

        if (oldValue instanceof byte[]) {
            return Arrays.equals((byte[])oldValue, (byte[])currentValue);
        }

        if (oldValue instanceof double[]) {
            return Arrays.equals((double[])oldValue, (double[])currentValue);
        }

        if (oldValue instanceof float[]) {
            return Arrays.equals((float[])oldValue, (float[])currentValue);
        }

        if (oldValue instanceof boolean[]) {
            return Arrays.equals((boolean[])oldValue, (boolean[])currentValue);
        }

        return Arrays.equals((Object[])oldValue, (Object[])currentValue);
    }
}
