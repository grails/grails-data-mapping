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
package org.springframework.datastore.mock.engine

import org.springframework.datastore.core.Session
import org.springframework.datastore.engine.AssociationIndexer
import org.springframework.datastore.engine.PropertyValueIndexer
import org.springframework.datastore.keyvalue.engine.AbstractKeyValueEntityPesister
import org.springframework.datastore.mapping.MappingContext
import org.springframework.datastore.mapping.PersistentEntity
import org.springframework.datastore.mapping.PersistentProperty
import org.springframework.datastore.mapping.types.Association
import org.springframework.datastore.query.Query
import org.springframework.datastore.mock.query.SimpleMapQuery

/**
 * A simple implementation of the {@link org.springframework.datastore.engine.EntityPersister} abstract class that backs onto an in-memory map.
 * Mainly used for mocking and testing scenarios
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class SimpleMapEntityPersister extends AbstractKeyValueEntityPesister<Map, Object>{

  Map<String, Map> datastore
  Map indices = [:]
  Long lastKey = 0

  SimpleMapEntityPersister(MappingContext context, PersistentEntity entity, Session session, datastore) {
    super(context, entity, session);
    this.datastore = datastore;
    datastore[getFamily(entity, entity.getMapping())] = [:]
  }

  Query createQuery() {
    return new SimpleMapQuery(session, super.getPersistentEntity(), this)
  }

  protected void deleteEntry(String family, Object key) {
    datastore[family].remove(key)
  }

  PropertyValueIndexer getPropertyIndexer(PersistentProperty property) {
    return new PropertyValueIndexer() {


      String getIndexRoot() {
          return "~${property.owner.name}:${property.name}"
      }

      void index(Object value, Object primaryKey) {
        def index = getIndexName(value)
        def indexed = indices[index]
        if(indexed == null) {
          indexed = []
          indices[index] = indexed
        }
        indexed << primaryKey
      }

      List query(Object value) {
        query(value, 0, -1)
      }

      List query(Object value, int offset, int max) {
        def index = getIndexName(value)
        def indexed = indices[index]
        if(indexed == null) {
          return Collections.emptyList()
        }
        return indexed[offset..max]

      }

      String getIndexName(Object value) {
        return "${indexRoot}:$value";
      }
    }
  }

  AssociationIndexer getAssociationIndexer(Association association) {
    return new AssociationIndexer() {

      private getIndexName(primaryKey) { "~${association.owner.name}:${association.name}:$primaryKey"}

      void index(Object primaryKey, List foreignKeys) {
        def indexed = getIndex(primaryKey)
        indexed.addAll(foreignKeys)

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

      void index(Object primaryKey, Object foreignKey) {
        def indexed = getIndex(primaryKey)
        indexed.add(foreignKey)

      }



      List query(Object primaryKey) {
        def index = getIndexName(primaryKey)
        def indexed = indices[index]
        if(indexed == null) {
          return Collections.emptyList()
        }
        return indexed
      }

      PersistentEntity getIndexedEntity() {
        return association.owner
      }
    }
  }

  protected Map createNewEntry(String family) {
    return [:];
  }

  protected Object getEntryValue(Map nativeEntry, String property) {
    return nativeEntry[property];
  }

  protected void setEntryValue(Map nativeEntry, String key, Object value) {
    nativeEntry[key] = value
  }

  protected Map retrieveEntry(PersistentEntity persistentEntity, String family, Serializable key) {
    return datastore[family].get(key)
  }

  protected Object generateIdentifier(PersistentEntity persistentEntity, Map id) {
    return ++lastKey;
  }


  protected Object storeEntry(PersistentEntity persistentEntity, Object storeId, Map nativeEntry) {
    def family = getFamily(persistentEntity, persistentEntity.getMapping())
    datastore[family].put(storeId, nativeEntry)
    return storeId
  }

  protected void updateEntry(PersistentEntity persistentEntity, Object key, Map entry) {
    def family = getFamily(persistentEntity, persistentEntity.getMapping())
    datastore[family].put(key, entry)

  }

  protected void deleteEntries(String family, List<Object> keys) {
    keys?.each { datastore[family].remove(it) }
  }
}
