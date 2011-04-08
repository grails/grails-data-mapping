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
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.engine.event.PostDeleteEvent;
import org.springframework.datastore.mapping.engine.event.PostInsertEvent;
import org.springframework.datastore.mapping.engine.event.PostLoadEvent;
import org.springframework.datastore.mapping.engine.event.PostUpdateEvent;
import org.springframework.datastore.mapping.engine.event.PreInsertEvent;
import org.springframework.datastore.mapping.engine.event.PreLoadEvent;
import org.springframework.datastore.mapping.engine.event.PreUpdateEvent;
import org.springframework.datastore.mapping.model.MappingContext;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.proxy.ProxyFactory;

/**
 * A {@link org.springframework.datastore.mapping.engine.Persister} specifically for persisting PersistentEntity instances.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public abstract class EntityPersister implements Persister {
    private PersistentEntity persistentEntity;
    private MappingContext mappingContext;
    protected Session session;
    protected org.springframework.datastore.mapping.proxy.ProxyFactory proxyFactory;
    protected ApplicationEventPublisher publisher;

    public EntityPersister(MappingContext mappingContext, PersistentEntity entity,
              Session session, ApplicationEventPublisher publisher) {
        this.persistentEntity = entity;
        this.mappingContext = mappingContext;
        this.session = session;
        this.publisher = publisher;
    }

    public Session getSession() {
        return session;
    }

    @SuppressWarnings("unchecked")
    public Object proxy(Serializable key) {
         return getProxyFactory().createProxy(session, getPersistentEntity().getJavaClass(), key);
    }

    public ProxyFactory getProxyFactory() {
        if (proxyFactory == null) {
            proxyFactory = mappingContext.getProxyFactory();
        }
        return proxyFactory;
    }

    /**
     * @return The MappingContext instance
     */
    public MappingContext getMappingContext() {
        return mappingContext;
    }

    /**
     * @return The PersistentEntity instance
     */
    public PersistentEntity getPersistentEntity() {
        return persistentEntity;
    }

    @SuppressWarnings("rawtypes")
    public Class getType() {
        return persistentEntity.getJavaClass();
    }

    /**
     * Obtains an objects identifer
     * @param obj The object
     * @return The identifier or null if it doesn't have one
     */
    public Serializable getObjectIdentifier(Object obj) {
        final ProxyFactory pf = getProxyFactory();
        if (pf.isProxy(obj)) {
            return pf.getIdentifier(obj);
        }
        return (Serializable) new EntityAccess(getPersistentEntity(), obj).getIdentifier();
    }

    /**
     * Obtains an objects identifer
     * @param obj The object
     */
    public void setObjectIdentifier(Object obj, Serializable id) {
        new EntityAccess(getPersistentEntity(), obj).setIdentifier(id);
    }

    /**
     * Persists an object returning the identifier
     *
     * @param obj The object to persist
     * @return The identifer
     */
    public final Serializable persist(Object obj) {
        if (!persistentEntity.isInstance(obj)) {
            final Persister persister = getSession().getPersister(obj);
            if (persister == null) {
                throw new IllegalArgumentException("Object [" + obj +
                     "] is not an instance supported by the persister for class [" +
                     getType().getName() + "]");
            }

            return persister.persist(obj);
        }

        return persistEntity(getPersistentEntity(), obj);
    }

    public List<Serializable> persist(@SuppressWarnings("rawtypes") Iterable objs) {
        return persistEntities(getPersistentEntity(), objs);
    }

    public List<Object> retrieveAll(Iterable<Serializable> keys) {
        return retrieveAllEntities(getPersistentEntity(), keys);
    }

    public List<Object> retrieveAll(Serializable[] keys) {
        return retrieveAllEntities(getPersistentEntity(), keys);
    }

    protected abstract List<Object> retrieveAllEntities(PersistentEntity pe, Serializable[] keys);

    protected abstract List<Object> retrieveAllEntities(PersistentEntity pe, Iterable<Serializable> keys);

    protected abstract List<Serializable> persistEntities(PersistentEntity pe, @SuppressWarnings("rawtypes") Iterable objs);

    public final Object retrieve(Serializable key) {
        if (key == null) {
            return null;
        }

        PersistentEntity entity = getPersistentEntity();

        Object o = retrieveEntity(entity, key);
        if (o == null) {
            return null;
        }

        publisher.publishEvent(new PostLoadEvent(session.getDatastore(),
                entity, new EntityAccess(entity, o)));

        return o;
    }

    /**
     * Retrieve a PersistentEntity for the given mappingContext and key
     *
     * @param pe The entity
     * @param key The key
     * @return The object or null if it doesn't exist
     */
    protected abstract Object retrieveEntity(PersistentEntity pe, Serializable key);

    /**
     * Persist the given persistent entity
     *
     * @param pe The PersistentEntity
     * @param obj
     * @return The generated key
     */
    protected abstract Serializable persistEntity(PersistentEntity pe, Object obj);

    public final void delete(@SuppressWarnings("rawtypes") Iterable objects) {
        if (objects == null) {
            return;
        }

        deleteEntities(getPersistentEntity(), objects);
    }

    public void delete(Object obj) {
        if (obj == null) {
            return;
        }

        deleteEntity(getPersistentEntity(), obj);
    }

    protected abstract void deleteEntity(PersistentEntity pe, Object obj);

    protected abstract void deleteEntities(PersistentEntity pe, @SuppressWarnings("rawtypes") Iterable objects);

    protected EntityAccess createEntityAccess(@SuppressWarnings("unused") PersistentEntity pe, Object obj) {
        return new EntityAccess(persistentEntity, obj);
    }

    protected Object newEntityInstance(@SuppressWarnings("hiding") PersistentEntity persistentEntity) {
        Object o = persistentEntity.newInstance();
        publisher.publishEvent(new PreLoadEvent(session.getDatastore(), getPersistentEntity(),
                new EntityAccess(persistentEntity, o)));
        return o;
    }

   /**
    * Fire the beforeInsert even on an entityAccess object and return true if the operation should be cancelled
    * @param persistentEntity The entity
    * @param entityAccess The entity access
    * @return true if the operation should be cancelled
    */
    public boolean cancelInsert(@SuppressWarnings("hiding") final PersistentEntity persistentEntity,
           final EntityAccess entityAccess) {
       PreInsertEvent event = new PreInsertEvent(session.getDatastore(), persistentEntity, entityAccess);
       publisher.publishEvent(event);
       return event.isCancelled();
   }

    public void firePostInsertEvent(@SuppressWarnings("hiding") final PersistentEntity persistentEntity,
            final EntityAccess entityAccess) {
        publisher.publishEvent(new PostInsertEvent(
                session.getDatastore(), persistentEntity, entityAccess));
    }

   /**
    * Fire the beforeUpdate even on an entityAccess object and return true if the operation should be cancelled
    * @param persistentEntity The entity
    * @param entityAccess The entity access
    * @return true if the operation should be cancelled
    */
    public boolean cancelUpdate(@SuppressWarnings("hiding") final PersistentEntity persistentEntity,
           final EntityAccess entityAccess) {
       PreUpdateEvent event = new PreUpdateEvent(session.getDatastore(), persistentEntity, entityAccess);
       publisher.publishEvent(event);
       return event.isCancelled();
   }

    public void firePostUpdateEvent(@SuppressWarnings("hiding") final PersistentEntity persistentEntity,
            final EntityAccess entityAccess) {
        publisher.publishEvent(new PostUpdateEvent(
                session.getDatastore(), persistentEntity, entityAccess));
    }

    public void firePostDeleteEvent(@SuppressWarnings("hiding") final PersistentEntity persistentEntity,
            final EntityAccess entityAccess) {
        publisher.publishEvent(new PostDeleteEvent(
                session.getDatastore(), persistentEntity, entityAccess));
    }

    protected boolean isVersioned(final EntityAccess ea) {

        if (!ea.getPersistentEntity().isVersioned()) {
            return false;
        }

        Class type = ea.getPropertyType("version");
        return Number.class.isAssignableFrom(type) || Date.class.isAssignableFrom(type);
    }

    protected void incrementVersion(final EntityAccess ea) {
        if (Number.class.isAssignableFrom(ea.getPropertyType("version"))) {
            ea.setProperty("version", ((Number)ea.getProperty("version")).longValue() + 1);
        }
        else {
            setDateVersion(ea);
        }
    }

    protected void setVersion(final EntityAccess ea) {
        if (Number.class.isAssignableFrom(ea.getPropertyType("version"))) {
            ea.setProperty("version", 0);
        }
        else {
            setDateVersion(ea);
        }
    }

    protected void setDateVersion(final EntityAccess ea) {
        if (Timestamp.class.isAssignableFrom(ea.getPropertyType("version"))) {
            ea.setProperty("version", new Timestamp(System.currentTimeMillis()));
        }
        else {
            ea.setProperty("version", new Date());
        }
    }
}
