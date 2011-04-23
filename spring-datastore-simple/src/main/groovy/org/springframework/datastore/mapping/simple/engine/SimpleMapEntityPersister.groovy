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
package org.springframework.datastore.mapping.simple.engine

import org.springframework.context.ApplicationEventPublisher
import org.springframework.datastore.mapping.core.OptimisticLockingException
import org.springframework.datastore.mapping.core.Session
import org.springframework.datastore.mapping.engine.AssociationIndexer
import org.springframework.datastore.mapping.engine.EntityAccess
import org.springframework.datastore.mapping.engine.EntityPersister
import org.springframework.datastore.mapping.engine.PropertyValueIndexer
import org.springframework.datastore.mapping.keyvalue.engine.AbstractKeyValueEntityPesister
import org.springframework.datastore.mapping.model.MappingContext
import org.springframework.datastore.mapping.model.PersistentEntity
import org.springframework.datastore.mapping.model.PersistentProperty
import org.springframework.datastore.mapping.model.types.Association
import org.springframework.datastore.mapping.query.Query
import org.springframework.datastore.mapping.simple.SimpleMapDatastore
import org.springframework.datastore.mapping.simple.query.SimpleMapQuery

/**
 * A simple implementation of the {@link org.springframework.datastore.mapping.engine.EntityPersister} abstract class that backs onto an in-memory map.
 * Mainly used for mocking and testing scenarios
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class SimpleMapEntityPersister extends AbstractKeyValueEntityPesister<Map, Object> {

    Map<String, Map> datastore
    Map indices
    Long lastKey = 0
    String family

    SimpleMapEntityPersister(MappingContext context, PersistentEntity entity, Session session,
                             SimpleMapDatastore datastore, ApplicationEventPublisher publisher) {
        super(context, entity, session, publisher)
        this.datastore = datastore.backingMap
        this.indices = datastore.indices
        family = getFamily(entity, entity.getMapping())
        if (this.datastore[family] == null) this.datastore[family] = [:]
    }

    protected PersistentEntity discriminatePersistentEntity(PersistentEntity persistentEntity, Map nativeEntry) {
        def disc = nativeEntry?.discriminator
        if (disc) {
            def childEntity = getMappingContext().getChildEntityByDiscriminator(persistentEntity.rootEntity, disc)
            if (childEntity) return childEntity
        }
        return persistentEntity
    }

    Query createQuery() {
        return new SimpleMapQuery(session, getPersistentEntity(), this)
    }

    protected void deleteEntry(String family, key) {
        datastore[family].remove(key)
    }

    PropertyValueIndexer getPropertyIndexer(PersistentProperty property) {
        return new PropertyValueIndexer() {

            String getIndexRoot() {
                return "~${property.owner.rootEntity.name}:${property.name}"
            }

            void deindex(value, primaryKey) {
                def index = getIndexName(value)
                def indexed = indices[index]
                if (indexed) {
                    indexed.remove(primaryKey)
                }
            }

            void index(value, primaryKey) {

                def index = getIndexName(value)
                def indexed = indices[index]
                if (indexed == null) {
                    indexed = []
                    indices[index] = indexed
                }
                if (!indexed.contains(primaryKey)) {
                    indexed << primaryKey
                }
            }

            List query(value) {
                query(value, 0, -1)
            }

            List query(value, int offset, int max) {
                def index = getIndexName(value)
                def indexed = indices[index]
                if (!indexed) {
                    return Collections.emptyList()
                }
                return indexed[offset..max]
            }

            String getIndexName(value) {
                return "${indexRoot}:$value"
            }
        }
    }

    AssociationIndexer getAssociationIndexer(Map nativeEntry, Association association) {
        return new AssociationIndexer() {

            private getIndexName(primaryKey) {
                "~${association.owner.name}:${association.name}:$primaryKey"
            }

            void index(primaryKey, List foreignKeys) {
                def indexed = getIndex(primaryKey)

                indexed.addAll(foreignKeys)
                def index = getIndexName(primaryKey)
                indexed = indexed.unique()
                indices[index] = indexed
            }

            private List getIndex(primaryKey) {
                def index = getIndexName(primaryKey)
                def indexed = indices[index]
                if (indexed == null) {
                    indexed = []
                    indices[index] = indexed
                }
                return indexed
            }

            void index(primaryKey, foreignKey) {
                def indexed = getIndex(primaryKey)
                if (!indexed.contains(foreignKey))
                    indexed.add(foreignKey)
            }

            List query(primaryKey) {
                def index = getIndexName(primaryKey)
                def indexed = indices[index]
                if (indexed == null) {
                    return Collections.emptyList()
                }
                return indexed
            }

            PersistentEntity getIndexedEntity() {
                return association.associatedEntity
            }
        }
    }

    protected Map createNewEntry(String family) {
        return [:]
    }

    protected getEntryValue(Map nativeEntry, String property) {
        return nativeEntry[property]
    }

    protected void setEntryValue(Map nativeEntry, String key, value) {
        if (mappingContext.isPersistentEntity(value)) {
            EntityPersister persister = session.getPersister(value)
            value = persister.getObjectIdentifier(value)
        }
        nativeEntry[key] = value
    }

    protected void setEmbedded(Map nativeEntry, String key, Map values) {
        nativeEntry[key] = values
    }

    protected Map getEmbedded( Map nativeEntry, String key) {
        nativeEntry[key]
    }

    protected Map retrieveEntry(PersistentEntity persistentEntity, String family, Serializable key) {
        datastore[family].get(key)
    }

    protected generateIdentifier(PersistentEntity persistentEntity, Map id) {
        if (persistentEntity.root) {
            lastKey++
            return persistentEntity.identity.type == String ? lastKey.toString() : lastKey
        }

        def root = persistentEntity.rootEntity
        session.getPersister(root).lastKey++
        def key = session.getPersister(root).lastKey
        return root.identity.type == String ? key.toString() : key
    }

    protected storeEntry(PersistentEntity persistentEntity, EntityAccess entityAccess, storeId, Map nativeEntry) {
        if (!persistentEntity.root) {
            nativeEntry.discriminator = persistentEntity.discriminator
        }
        datastore[family].put(storeId, nativeEntry)
        updateInheritanceHierarchy(persistentEntity, storeId, nativeEntry)
        return storeId
    }

    private updateInheritanceHierarchy(PersistentEntity persistentEntity, storeId, Map nativeEntry) {
        def parent = persistentEntity.parentEntity
        while (parent != null) {

            def f = getFamily(parent, parent.mapping)
            def parentEntry = datastore[f]
            if (parentEntry == null) {
                parentEntry = [:]
                datastore[f] = parentEntry
            }
            parentEntry.put(storeId, nativeEntry)
            parent = parent.parentEntity
        }
    }

    protected void updateEntry(PersistentEntity persistentEntity, EntityAccess entityAccess, key, Map entry) {
        def family = getFamily(persistentEntity, persistentEntity.getMapping())
        def existing = datastore[family].get(key)

        if (isVersioned(entityAccess)) {
            if (existing == null) {
                setVersion entityAccess
            }
            else {
                def oldVersion = existing.version
                def currentVersion = entityAccess.getProperty('version')
                if (Number.isAssignableFrom(entityAccess.getPropertyType('version'))) {
                    oldVersion = existing.version.toLong()
                    currentVersion = entityAccess.getProperty('version').toLong()
                }
                if (!oldVersion.equals(currentVersion)) {
                    throw new OptimisticLockingException(persistentEntity, key)
                }
                incrementVersion(entityAccess)
            }
        }

        if (existing == null) {
            datastore[family].put(key, entry)
        }
        else {
            existing.putAll(entry)
        }
        updateInheritanceHierarchy(persistentEntity, key, entry)
    }

    protected void deleteEntries(String family, List<Object> keys) {
        keys?.each {
            datastore[family].remove(it)
            def parent = persistentEntity.parentEntity
            while (parent != null) {
                def f = getFamily(parent, parent.mapping)
                datastore[f].remove(it)
                parent = parent.parentEntity
            }
        }
    }
}
