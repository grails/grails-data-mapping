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
package org.grails.inconsequential.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract implementation of the MappingContext interface
 * 
 * @author Graeme Rocher
 * @since 1.0
 */
public abstract class AbstractMappingContext implements MappingContext {

    protected List<PersistentEntity> persistentEntities = new ArrayList<PersistentEntity>();
    protected Map<String,PersistentEntity>  persistentEntitiesByName = new HashMap<String,PersistentEntity>();

    public final PersistentEntity addPersistentEntity(Class javaClass) {
        PersistentEntity entity = createPersistentEntity(javaClass);
        entity.initialize();
        persistentEntities.remove(entity); persistentEntities.add(entity);
        persistentEntitiesByName.put(entity.getName(), entity);
        return entity;
    }

    protected abstract PersistentEntity createPersistentEntity(Class javaClass);

    public List<PersistentEntity> getPersistentEntities() {
        return persistentEntities;
    }

    public PersistentEntity getPersistentEntity(String name) {
        return persistentEntitiesByName.get(name);
    }
}
