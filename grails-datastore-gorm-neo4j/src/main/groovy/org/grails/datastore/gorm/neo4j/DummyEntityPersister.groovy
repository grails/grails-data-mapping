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
package org.grails.datastore.gorm.neo4j

import org.grails.datastore.mapping.engine.EntityPersister
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.query.Query
import org.grails.datastore.mapping.model.MappingContext

/**
 * dummy implementation of {@link org.grails.datastore.mapping.engine.EntityPersister}
 * The Neo4j gorm implementation does all persistence stuff in {@link Neo4jSession}, so there's no
 * need for a {@link org.grails.datastore.mapping.engine.EntityPersister}, except on issue:
 * {@link org.grails.datastore.gorm.proxy.GroovyProxyFactory} references
 * {@link org.grails.datastore.mapping.engine.EntityPersister#setObjectIdentifier} so {@link Neo4jSession#proxy} needs
 * to pass a valid EntityPersister - that's the purpose of this class.
 *
 * @author Stefan Armbruster <stefan@armbruster-it.de>
 */
class DummyEntityPersister extends EntityPersister {

    public DummyEntityPersister(MappingContext mappingContext, PersistentEntity entity) {
        super(mappingContext, entity, null, null)
    }

    @Override
    protected List<Object> retrieveAllEntities(PersistentEntity pe, Serializable[] keys) {
        throw new UnsupportedOperationException()
    }

    @Override
    protected List<Object> retrieveAllEntities(PersistentEntity pe, Iterable<Serializable> keys) {
        throw new UnsupportedOperationException()
    }

    @Override
    protected List<Serializable> persistEntities(PersistentEntity pe, Iterable objs) {
        throw new UnsupportedOperationException()
    }

    @Override
    protected Object retrieveEntity(PersistentEntity pe, Serializable key) {
        throw new UnsupportedOperationException()
    }

    @Override
    protected Serializable persistEntity(PersistentEntity pe, Object obj) {
        throw new UnsupportedOperationException()
    }

    @Override
    protected void deleteEntity(PersistentEntity pe, Object obj) {
        throw new UnsupportedOperationException()
    }

    @Override
    protected void deleteEntities(PersistentEntity pe, Iterable objects) {
        throw new UnsupportedOperationException()
    }

    @Override
    Query createQuery() {
        throw new UnsupportedOperationException()
    }

    @Override
    Serializable refresh(Object o) {
        throw new UnsupportedOperationException()
    }

}
