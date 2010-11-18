/*
 * Copyright (c) 2010 by NPC International, Inc.
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

package org.springframework.datastore.mapping.riak.engine;

import org.springframework.core.convert.ConversionService;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.engine.AssociationIndexer;
import org.springframework.datastore.mapping.engine.PropertyValueIndexer;
import org.springframework.datastore.mapping.keyvalue.engine.AbstractKeyValueEntityPesister;
import org.springframework.datastore.mapping.model.MappingContext;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.model.PersistentProperty;
import org.springframework.datastore.mapping.model.types.Association;
import org.springframework.datastore.mapping.proxy.EntityProxy;
import org.springframework.datastore.mapping.query.Query;
import org.springframework.datastore.mapping.riak.RiakEntry;
import org.springframework.datastore.mapping.riak.collection.RiakEntityIndex;
import org.springframework.datastore.mapping.riak.query.RiakQuery;
import org.springframework.datastore.riak.core.RiakTemplate;

import java.io.Serializable;
import java.util.*;

/**
 * @author Jon Brisbin <jon.brisbin@npcinternational.com>
 */
@SuppressWarnings({"unchecked"})
public class RiakEntityPersister extends AbstractKeyValueEntityPesister<Map, Long> {

  private RiakTemplate riakTemplate;

  public RiakEntityPersister(MappingContext context, PersistentEntity entity, Session session, final RiakTemplate riakTemplate) {
    super(context, entity, session);
    this.riakTemplate = riakTemplate;
  }

  @Override
  protected void deleteEntry(String family, Long key) {
    riakTemplate.deleteKeys(String.format("%s:%s", family, key));
  }

  @Override
  protected Long generateIdentifier(PersistentEntity persistentEntity, Map id) {
    return UUID.randomUUID().getLeastSignificantBits();
  }

  public RiakEntityIndex getAllEntityIndex() {
    return new RiakEntityIndex(riakTemplate, getEntityFamily());
  }

  @Override
  public PropertyValueIndexer getPropertyIndexer(PersistentProperty property) {
    return new RiakPropertyValueIndexer(riakTemplate, getMappingContext(), this, property);
  }

  @Override
  public AssociationIndexer getAssociationIndexer(Map nativeEntry, Association association) {
    return new RiakAssociationIndexer(riakTemplate, getMappingContext().getConversionService(), association);
  }

  @Override
  protected Map createNewEntry(String family) {
    return new RiakEntry(family);
  }

  @Override
  protected Object getEntryValue(Map nativeEntry, String property) {
    PersistentProperty prop = getPersistentEntity().getPropertyByName(property);
    if (prop.getType() == Date.class) {
      return new Date(Long.parseLong(nativeEntry.get(property).toString()));
    } else if (prop.getType() == Calendar.class) {
      Calendar c = Calendar.getInstance();
      c.setTime(new Date(Long.parseLong(nativeEntry.get(property).toString())));
      return c;
    } else if (prop.getType() == Boolean.class) {
      return new Boolean(nativeEntry.get(property).toString());
    } else {
      return nativeEntry.get(property);
    }
  }

  @Override
  protected void setEntryValue(Map nativeEntry, String key, Object value) {
    if (null != value) {
      if (value instanceof Date) {
        nativeEntry.put(key, ((Date) value).getTime());
      } else if (value instanceof Calendar) {
        nativeEntry.put(key, ((Calendar) value).getTime().getTime());
      } else if (value instanceof Boolean) {
        nativeEntry.put(key, value);
      } else if (shouldConvert(value)) {
        final ConversionService conversionService = getMappingContext().getConversionService();
        nativeEntry.put(key, conversionService.convert(value, String.class));
      }
    }
  }

  @Override
  protected Map retrieveEntry(PersistentEntity persistentEntity, String family, Serializable key) {
    return riakTemplate.getAsType(String.format("%s:%s",
        family,
        (key instanceof Long ? (Long) key : Long.parseLong(key.toString()))), Map.class);
  }

  @Override
  protected Long storeEntry(PersistentEntity persistentEntity, Long storeId, Map nativeEntry) {
    String family = getRootFamily(persistentEntity);
    riakTemplate.set(String.format("%s:%s", family, storeId), nativeEntry);
    return storeId;
  }

  @Override
  protected void updateEntry(PersistentEntity persistentEntity, Long key, Map entry) {
    String family = getRootFamily(persistentEntity);
    riakTemplate.set(String.format("%s:%s", family, key), entry);
  }

  @Override
  protected void deleteEntries(String family, List<Long> keys) {
    for (Long key : keys) {
      riakTemplate.deleteKeys(String.format("%s:%s", family, key));
    }
  }

  protected boolean shouldConvert(Object value) {
    return !getMappingContext().isPersistentEntity(value) && !(value instanceof EntityProxy);
  }

  public Query createQuery() {
    return new RiakQuery(session, getPersistentEntity(), riakTemplate);
  }

  protected String getRootFamily(PersistentEntity entity) {
    String family = getFamily(entity, entity.getMapping());
    if (!entity.isRoot()) {
      PersistentEntity root = entity.getRootEntity();
      family = getFamily(root, root.getMapping());
    }
    return family;
  }
}
