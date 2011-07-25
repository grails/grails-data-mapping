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
package org.grails.datastore.mapping.gemfire.engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;

import javax.persistence.CascadeType;

import org.grails.datastore.mapping.core.OptimisticLockingException;
import org.grails.datastore.mapping.core.Session;
import org.grails.datastore.mapping.engine.EntityAccess;
import org.grails.datastore.mapping.engine.LockableEntityPersister;
import org.grails.datastore.mapping.engine.event.AbstractPersistenceEvent;
import org.grails.datastore.mapping.engine.event.PreDeleteEvent;
import org.grails.datastore.mapping.engine.event.PreInsertEvent;
import org.grails.datastore.mapping.engine.event.PreLoadEvent;
import org.grails.datastore.mapping.engine.event.PreUpdateEvent;
import org.grails.datastore.mapping.gemfire.GemfireDatastore;
import org.grails.datastore.mapping.gemfire.GemfireSession;
import org.grails.datastore.mapping.gemfire.query.GemfireQuery;
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValue;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.types.Association;
import org.grails.datastore.mapping.model.types.OneToMany;
import org.grails.datastore.mapping.model.types.ToOne;
import org.grails.datastore.mapping.query.Query;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.gemfire.GemfireCallback;
import org.springframework.data.gemfire.GemfireTemplate;

import com.gemstone.gemfire.GemFireCheckedException;
import com.gemstone.gemfire.GemFireException;
import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.CacheFactory;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.distributed.internal.InternalDistributedSystem;
import com.gemstone.gemfire.internal.cache.PartitionedRegion;

/**
 * A persister capable of storing objects in a Gemfire region.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class GemfireEntityPersister extends LockableEntityPersister {

    private GemfireDatastore gemfireDatastore;
    private Map<Object, Lock> distributedLocksHeld = new ConcurrentHashMap<Object, Lock>();
    private static final String CASCADE_PROCESSED = "cascade.processed";
    private static AtomicInteger identifierGenerator = new AtomicInteger(0);

    public GemfireEntityPersister(MappingContext mappingContext, PersistentEntity entity,
              Session session, ApplicationEventPublisher publisher) {
        super(mappingContext, entity, session, publisher);
        this.gemfireDatastore = (GemfireDatastore) session.getDatastore();
    }

    @Override
    public Object lock(final Serializable id) throws CannotAcquireLockException {
        final GemfireTemplate template = gemfireDatastore.getTemplate(getPersistentEntity());

        return template.execute(new GemfireCallback() {
            public Object doInGemfire(Region region) throws GemFireCheckedException, GemFireException {
                final Lock lock = region.getDistributedLock(id);
                lock.lock();
                final Object o = region.get(id);
                distributedLocksHeld.put(o, lock);
                return o;
            }
        });
    }

    @Override
    public Object lock(final Serializable id, final int timeout) throws CannotAcquireLockException {
        final GemfireTemplate template = gemfireDatastore.getTemplate(getPersistentEntity());

        return template.execute(new GemfireCallback() {
            public Object doInGemfire(Region region) throws GemFireCheckedException, GemFireException {
                final Lock lock = region.getDistributedLock(id);
                try {
                    if (lock.tryLock(timeout, TimeUnit.SECONDS)) {
                        final Object o = region.get(id);
                        distributedLocksHeld.put(o, lock);
                        return o;
                    }
                    throw new CannotAcquireLockException("Timeout acquiring Gemfire lock on object type ["+getPersistentEntity()+"] with identifier ["+id+"]");
                } catch (InterruptedException e) {
                    throw new CannotAcquireLockException("Cannot acquire Gemfire lock on object type ["+getPersistentEntity()+"] with identifier ["+id+"]: " + e.getMessage(), e);
                }
            }
        });
    }

    @Override
    public boolean isLocked(Object o) {
        return distributedLocksHeld.containsKey(o);
    }

    @Override
    public void unlock(Object o) {
        final Lock lock = distributedLocksHeld.get(o);
        if (lock != null) {
            lock.unlock();
        }
    }

    @Override
    protected List<Object> retrieveAllEntities(PersistentEntity persistentEntity, final Serializable[] keys) {
        final GemfireTemplate template = gemfireDatastore.getTemplate(persistentEntity);
        return (List<Object>) template.execute(new GemfireCallback() {
            public Object doInGemfire(Region region) throws GemFireCheckedException, GemFireException {
                return region.getAll(Arrays.asList(keys));
            }
        });
    }

    @Override
    protected List<Object> retrieveAllEntities(PersistentEntity persistentEntity, final Iterable<Serializable> keys) {
        final GemfireTemplate template = gemfireDatastore.getTemplate(persistentEntity);
        return (List<Object>) template.execute(new GemfireCallback() {
            public Object doInGemfire(Region region) throws GemFireCheckedException, GemFireException {
                if (keys instanceof Collection) {
                    return getListOfValues(region.getAll((Collection) keys));
                }
                Collection keyList = new ArrayList();
                for (Serializable key : keys) {
                    keyList.add(key);
                }
                return getListOfValues(region.getAll(keyList));
            }

            List getListOfValues(final Map all) {
                if (all != null) {
                    return new ArrayList(all.values());
                }
                return Collections.emptyList();
            }
        });
    }

    @Override
    protected List<Serializable> persistEntities(final PersistentEntity persistentEntity, Iterable objs) {
        final GemfireTemplate template = gemfireDatastore.getTemplate(persistentEntity);
        final Map putMap = new HashMap();
        List<Serializable> identifiers = new ArrayList<Serializable>();
        final Map<Object, EntityAccess> entityAccessObjects = new HashMap<Object, EntityAccess>();

        final Map<Object, Boolean> updates = new HashMap<Object, Boolean>();
        for (Object obj : objs) {
            final EntityAccess access = createEntityAccess(persistentEntity,obj);
            entityAccessObjects.put(obj, access);
            Object identifier = access.getIdentifier();
            boolean isUpdate = true;
            if (identifier == null) {
                identifier = generateIdentifier(persistentEntity, access);
                isUpdate = false;
            }

            AbstractPersistenceEvent event = isUpdate ?
                new PreUpdateEvent(session.getDatastore(), persistentEntity, access) :
                new PreInsertEvent(session.getDatastore(), persistentEntity, access);
            updates.put(obj, isUpdate);
            publisher.publishEvent(event);
            if (event.isCancelled()) {
                break;
            }

            putMap.put(identifier, obj);
            identifiers.add((Serializable) identifier);
        }

        template.execute(new GemfireCallback() {

            public Object doInGemfire(Region region) throws GemFireCheckedException, GemFireException {
                region.putAll(putMap);

                if (!persistentEntity.isRoot()) {
                    doWithParents(persistentEntity, new GemfireCallback() {
                        @SuppressWarnings("hiding")
                        public Object doInGemfire(Region region) throws GemFireCheckedException, GemFireException {
                            region.putAll(putMap);
                            return null;
                        }
                    });
                }

                for (Object id : putMap.keySet()) {
                    Object obj = putMap.get(id);
                    final EntityAccess access = entityAccessObjects.get(obj);
                    if (access != null) {
                        cascadeSaveOrUpdate(persistentEntity, obj, access);
                        if (updates.get(obj)) {
                            firePostUpdateEvent(persistentEntity, access);
                        }
                        else {
                            firePostInsertEvent(persistentEntity, access);
                        }
                    }
                }

                return null;
            }
        });

        return identifiers;
    }

    @Override
    protected Object retrieveEntity(final PersistentEntity persistentEntity, final Serializable key) {

        final GemfireTemplate template = gemfireDatastore.getTemplate(persistentEntity);
        return template.execute(new GemfireCallback() {
            public Object doInGemfire(Region region) throws GemFireCheckedException, GemFireException {
                final Class idType = persistentEntity.getIdentity().getType();
                Object lookupKey = getMappingContext().getConversionService().convert(key, idType);
                final Object entry = region.get(lookupKey);

                if (entry != null) {
                    publisher.publishEvent(new PreLoadEvent(session.getDatastore(), getPersistentEntity(),
                         new EntityAccess(persistentEntity, entry)));
                }

                for (Association association : persistentEntity.getAssociations()) {
                    if (association instanceof OneToMany) {
                        final EntityAccess ea = createEntityAccess(persistentEntity, entry);
                        final String propertyName = association.getName();
                        final Object currentState = ea.getProperty(propertyName);
                        if (currentState == null) {
                            initializeCollectionState(association, ea, propertyName);
                        }
                    }
                }
                return entry;
            }
        });
    }

    private Object initializeCollectionState(Association association, EntityAccess ea, String propertyName) {
        if (Set.class.isAssignableFrom(association.getType())) {
            final HashSet set = new HashSet();
            ea.setProperty(propertyName, set);
            return set;
        }

        if (List.class.isAssignableFrom(association.getType())) {
            final ArrayList list = new ArrayList();
            ea.setProperty(propertyName, list);
            return list;
        }

        if (Map.class.isAssignableFrom(association.getType())) {
            final HashMap map = new HashMap();
            ea.setProperty(propertyName, map);
            return map;
        }

        return null;
    }

    @Override
    protected Serializable persistEntity(final PersistentEntity persistentEntity, final Object obj) {
        final EntityAccess access = createEntityAccess(persistentEntity,obj);
        Object identifier = access.getIdentifier();
        boolean isUpdate = true;
        if (identifier == null) {
            identifier = generateIdentifier(persistentEntity, access);
            isUpdate = false;
        }
        final Object finalId = identifier;
        final GemfireTemplate template = gemfireDatastore.getTemplate(persistentEntity);

        final boolean finalIsUpdate = isUpdate;
        template.execute(new GemfireCallback() {

            public Object doInGemfire(Region region) throws GemFireCheckedException, GemFireException {

                AbstractPersistenceEvent event = finalIsUpdate ?
                        new PreUpdateEvent(session.getDatastore(), persistentEntity, access) :
                        new PreInsertEvent(session.getDatastore(), persistentEntity, access);
                publisher.publishEvent(event);
                if (event.isCancelled()) {
                    return finalId;
                }

                if (finalIsUpdate && isVersioned(access)) {
                    // TODO this should be done with a CAS approach if possible
                    checkVersion(region, access, persistentEntity, finalId);
                }

                region.put(finalId, obj);
                if (!persistentEntity.isRoot()) {
                    doWithParents(persistentEntity, new GemfireCallback() {
                        @SuppressWarnings("hiding")
                        public Object doInGemfire(Region region) throws GemFireCheckedException, GemFireException {
                            region.put(finalId, obj);
                            return null;
                        }
                    });
                }

                cascadeSaveOrUpdate(persistentEntity, obj, access);

                if (finalIsUpdate) {
                    firePostUpdateEvent(persistentEntity, access);
                }
                else {
                    firePostInsertEvent(persistentEntity, access);
                }

                return null;
            }
        });
        return (Serializable) identifier;
    }

    protected void checkVersion(Region region, EntityAccess access,
                                PersistentEntity persistentEntity, Object id) {

        final Class idType = persistentEntity.getIdentity().getType();
        Object lookupKey = getMappingContext().getConversionService().convert(id, idType);
        Object previous = region.get(lookupKey);

        Object oldVersion = new EntityAccess(persistentEntity, previous).getProperty("version");
        Object currentVersion = access.getProperty("version");
        if (Number.class.isAssignableFrom(access.getPropertyType("version"))) {
            oldVersion = ((Number)oldVersion).longValue();
            currentVersion = ((Number)currentVersion).longValue();
        }

        if (oldVersion != null && currentVersion != null && !oldVersion.equals(currentVersion)) {
            throw new OptimisticLockingException(persistentEntity, id);
        }

        incrementVersion(access);
    }

    private void cascadeSaveOrUpdate(PersistentEntity persistentEntity, Object obj, EntityAccess access) {
        final List<Association> associations = persistentEntity.getAssociations();
        for (Association association : associations) {
            if (association.doesCascade(CascadeType.PERSIST)) {
                @SuppressWarnings("hiding") final Session session = getSession();
                String processKey = association + ">" + obj;
                if (association instanceof ToOne) {
                    final Object associatedObject = access.getProperty(association.getName());

                    if (associatedObject != null && !associatedObject.equals(obj)) {
                        if (session.getAttribute(processKey, CASCADE_PROCESSED) == null) {
                            session.setAttribute(processKey, CASCADE_PROCESSED, true);
                            this.session.persist(associatedObject);
                            autoAssociateInverseSide(obj, association, associatedObject);
                        }
                    }
                    else {
                        session.setAttribute(processKey, CASCADE_PROCESSED, false);
                    }
                }
                else if (association instanceof OneToMany) {
                    if (session.getAttribute(processKey, CASCADE_PROCESSED) == Boolean.TRUE) {
                        session.setAttribute(processKey, CASCADE_PROCESSED, Boolean.TRUE);
                        Object associatedObjects = access.getProperty(association.getName());
                        if (associatedObjects instanceof Iterable) {
                            final Iterable iterable = (Iterable) associatedObjects;
                            for (Object associatedObject : iterable) {
                                autoAssociateInverseSide(obj, association, associatedObject);
                            }
                            session.persist(iterable);
                        }
                    }
                    else {
                        session.setAttribute(processKey, CASCADE_PROCESSED, false);
                    }
                }
            }
        }
    }

    private void autoAssociateInverseSide(Object obj, Association association, Object associatedObject) {
        if (association.isBidirectional()) {
            final Association inverseSide = association.getInverseSide();
            if (inverseSide instanceof ToOne) {
                final EntityAccess associationAccess = createEntityAccess(association.getAssociatedEntity(), associatedObject);
                associationAccess.setProperty(inverseSide.getName(), obj);
            }
            else if (inverseSide instanceof OneToMany) {
                final EntityAccess associationAccess = createEntityAccess(association.getAssociatedEntity(), associatedObject);
                Object collectionObject = associationAccess.getProperty(inverseSide.getName());
                if (collectionObject == null) {
                    collectionObject = initializeCollectionState(inverseSide, associationAccess, inverseSide.getName());
                }

                if (collectionObject instanceof Collection) {
                    final Collection collection = (Collection) collectionObject;
                    if (!collection.contains(obj))
                        collection.add(obj);
                }
            }
        }
    }

    private void cascadeDelete(PersistentEntity persistentEntity, Object obj, EntityAccess access) {
        final List<Association> associations = persistentEntity.getAssociations();
        for (Association association : associations) {
            if (association.doesCascade(CascadeType.REMOVE)) {
                if (association instanceof ToOne) {
                    ToOne toOne = (ToOne) association;

                    final Object associatedObject = access.getProperty(toOne.getName());
                    if (associatedObject != null && !associatedObject.equals(obj)) {
                        session.delete(associatedObject);
                    }
                }
                else if (association instanceof OneToMany) {
                    Object associatedObjects = access.getProperty(association.getName());
                    if (associatedObjects instanceof Iterable) {
                        session.delete((Iterable)associatedObjects);
                    }
                }
            }
        }
    }

    private void doWithParents(PersistentEntity persistentEntity, GemfireCallback gemfireCallback) {
        if (!persistentEntity.isRoot()) {
            PersistentEntity parentEntity = persistentEntity.getParentEntity();
            do {
                GemfireTemplate parentTemplate = gemfireDatastore.getTemplate(parentEntity);
                parentTemplate.execute(gemfireCallback);
                parentEntity = parentEntity.getParentEntity();
            }
            while(parentEntity != null && !(parentEntity.isRoot()));
        }
    }

    private Object generateIdentifier(final PersistentEntity persistentEntity, final EntityAccess access) {
        final GemfireTemplate template = gemfireDatastore.getTemplate(persistentEntity);
        return template.execute(new GemfireCallback() {

            public Object doInGemfire(Region region) throws GemFireCheckedException, GemFireException {

                KeyValue mf = (KeyValue)gemfireDatastore.getMappingContext().getMappingFactory().createMappedForm(persistentEntity.getIdentity());
                if ("uuid".equals(mf.getGenerator())) {
                    String uuid = UUID.randomUUID().toString();
                    access.setIdentifier(uuid);
                    return uuid;
                }

                Cache cache = CacheFactory.getAnyInstance();
                final int uuid = PartitionedRegion.generatePRId(
                        (InternalDistributedSystem)cache.getDistributedSystem(),cache);
                if (uuid == 0) {
                    throw new DataAccessResourceFailureException("Unable to generate Gemfire UUID");
                }
                long finalId = identifierGenerator.getAndIncrement() + uuid;
                access.setIdentifier(finalId);
                return finalId;
            }
        });
    }

    @Override
    protected void deleteEntity(final PersistentEntity persistentEntity, final Object obj) {
        final EntityAccess access = createEntityAccess(persistentEntity, obj);

        final Object identifier = access.getIdentifier();
        final GemfireTemplate template = gemfireDatastore.getTemplate(persistentEntity);

        template.execute(new GemfireCallback() {

            public Object doInGemfire(Region region) throws GemFireCheckedException, GemFireException {
                PreDeleteEvent event = new PreDeleteEvent(session.getDatastore(), persistentEntity, access);
                publisher.publishEvent(event);
                if (event.isCancelled()) {
                    return null;
                }

                region.remove(identifier);
                if (!persistentEntity.isRoot()) {
                    doWithParents(persistentEntity, new GemfireCallback() {
                        @SuppressWarnings("hiding")
                        public Object doInGemfire(Region region) throws GemFireCheckedException, GemFireException {
                            region.remove(identifier);
                            return null;
                        }
                    });
                }
                cascadeDelete(persistentEntity, obj, access);
                firePostDeleteEvent(persistentEntity, access);
                return null;
            }
        });
    }

    @Override
    protected void deleteEntities(PersistentEntity persistentEntity, Iterable objects) {
        for (Object object : objects) {
            deleteEntity(persistentEntity, object);
        }
    }

    public Query createQuery() {
        return new GemfireQuery((GemfireSession) getSession(), getPersistentEntity());
    }

    public Serializable refresh(Object o) {
        return (Serializable)createEntityAccess(getPersistentEntity(), o).getIdentifier();
    }
}
