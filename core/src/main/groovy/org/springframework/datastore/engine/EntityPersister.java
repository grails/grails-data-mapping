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
package org.springframework.datastore.engine;

import org.springframework.datastore.mapping.MappingContext;
import org.springframework.datastore.mapping.PersistentEntity;

import java.io.Serializable;
import java.util.List;

/**
 * A Persister specified to PersistentEntity instances
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public abstract class EntityPersister implements Persister {
    private PersistentEntity persistentEntity;

    public EntityPersister(PersistentEntity entity) {
        this.persistentEntity = entity;
    }

    public PersistentEntity getPersistentEntity() {
        return persistentEntity;
    }

    public Class getType() {
        return persistentEntity.getJavaClass();
    }

    public final Serializable persist(MappingContext context, Object obj) {
        if(!persistentEntity.isInstance(obj)) throw new IllegalArgumentException("Object ["+obj+"] is not an instance supported by the persister for class ["+getType().getName()+"]");

        return persistEntity(context, getPersistentEntity(), new EntityAccess(obj));
    }

    public List<Serializable> persist(MappingContext context, Iterable objs) {
        return persistEntities(context, getPersistentEntity(), objs);
    }

    public List<Object> retrieveAll(MappingContext context, Iterable<Serializable> keys) {
        return retrieveAllEntities(context, getPersistentEntity(), keys);
    }

    protected abstract List<Object> retrieveAllEntities(MappingContext context, PersistentEntity persistentEntity, Iterable<Serializable> keys);

    protected abstract List<Serializable> persistEntities(MappingContext context, PersistentEntity persistentEntity, Iterable objs);

    public final Object retrieve(MappingContext context, Serializable key) {
        if(key == null) return null;
        return retrieveEntity(context, getPersistentEntity(), key);
    }

    /**
     * Retrieve a PersistentEntity for the given context and key
     *
     * @param context The context
     * @param persistentEntity The entity
     * @param key The key
     * @return The object or null if it doesn't exist
     */
    protected abstract Object retrieveEntity(MappingContext context, PersistentEntity persistentEntity, Serializable key);

    /**
     * Persist the given persistent entity
     *
     * @param context The context
     * @param persistentEntity The PersistentEntity
     * @param entityAccess An object that allows easy access to the entities properties
     * @return The generated key
     */
    protected abstract Serializable persistEntity(MappingContext context, PersistentEntity persistentEntity, EntityAccess entityAccess);

    public final void delete(MappingContext context, Iterable objects) {
        if(objects != null) {
            deleteEntities(context, getPersistentEntity(), objects);
        }
    }

    public void delete(MappingContext mappingContext, Object obj) {
        if(obj != null) {
            deleteEntity(mappingContext, getPersistentEntity(), obj);
        }
    }

    protected abstract void deleteEntity(MappingContext mappingContext, PersistentEntity persistentEntity, Object obj);

    protected abstract void deleteEntities(MappingContext context, PersistentEntity persistentEntity, Iterable objects);
}

