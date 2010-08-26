/* Copyright 2004-2005 the original author or authors.
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
package org.springframework.datastore.mapping;

import org.springframework.validation.Validator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Abstract implementation of the MappingContext interface
 * 
 * @author Graeme Rocher
 * @since 1.0
 */
public abstract class AbstractMappingContext implements MappingContext {

    protected Collection<PersistentEntity> persistentEntities = new ConcurrentLinkedQueue<PersistentEntity>();
    protected Map<String,PersistentEntity>  persistentEntitiesByName = new ConcurrentHashMap<String,PersistentEntity>();
    protected Map<PersistentEntity,Validator>  entityValidators = new ConcurrentHashMap<PersistentEntity, Validator>();

    public Validator getEntityValidator(PersistentEntity entity) {
        if(entity != null) {
            return entityValidators.get(entity);
        }
        return null;
    }

    public void addEntityValidator(PersistentEntity entity, Validator validator) {
        if(entity != null && validator != null) {
            entityValidators.put(entity, validator);
        }
    }

    public final PersistentEntity addPersistentEntity(Class javaClass) {
        if(javaClass == null) throw new IllegalArgumentException("PersistentEntity class cannot be null");
        PersistentEntity entity = createPersistentEntity(javaClass);

        persistentEntities.remove(entity); persistentEntities.add(entity);
        persistentEntitiesByName.put(entity.getName(), entity);
        entity.initialize();

        return entity;
    }

    protected abstract PersistentEntity createPersistentEntity(Class javaClass);

    public Collection<PersistentEntity> getPersistentEntities() {
        return persistentEntities;
    }

    public boolean isPersistentEntity(Class type) {
        return type != null && getPersistentEntity(type.getName()) != null;

    }

    public boolean isPersistentEntity(Object value) {
        return value != null && isPersistentEntity(value.getClass());
    }

    public PersistentEntity getPersistentEntity(String name) {
        final int proxyIndicator = name.indexOf("_$$_");
        if(proxyIndicator > -1) {
            name = name.substring(0, proxyIndicator);
        }
        
        return persistentEntitiesByName.get(name);
    }
}
