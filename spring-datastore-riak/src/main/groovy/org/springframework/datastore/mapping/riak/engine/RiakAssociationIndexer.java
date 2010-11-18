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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.datastore.mapping.engine.AssociationIndexer;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.model.types.Association;
import org.springframework.datastore.riak.core.RiakTemplate;
import org.springframework.datastore.riak.core.SimpleBucketKeyPair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Jon Brisbin <jon.brisbin@npcinternational.com>
 */
public class RiakAssociationIndexer implements AssociationIndexer<Long, Long> {

  private final Logger log = LoggerFactory.getLogger(getClass());
  private RiakTemplate riakTemplate;
  private ConversionService conversionService;
  private Association association;
  PersistentEntity owner;
  PersistentEntity child;

  public RiakAssociationIndexer(RiakTemplate riakTemplate, ConversionService conversionService, Association association) {
    this.riakTemplate = riakTemplate;
    this.conversionService = conversionService;
    this.association = association;
    this.owner = association.getOwner();
    this.child = association.getAssociatedEntity();
  }

  public void index(Long primaryKey, List<Long> foreignKeys) {
    for (Long foreignKey : foreignKeys) {
      link(foreignKey, primaryKey);
    }
  }

  public void index(Long primaryKey, Long foreignKey) {
    link(foreignKey, primaryKey);
  }

  protected void link(Long childKey, Long ownerKey) {
    SimpleBucketKeyPair<String, Long> bkpChild = new SimpleBucketKeyPair<String, Long>(child.getName(), childKey);
    SimpleBucketKeyPair<String, Long> bkpOwner = new SimpleBucketKeyPair<String, Long>(owner.getName(), ownerKey);
    riakTemplate.link(bkpChild, bkpOwner, association.getName());
  }

  public List<Long> query(Long primaryKey) {
    String bucketName = String.format("%s.%s.%s", owner.getName(), primaryKey, association.getName());
    Map<String, Object> schema = riakTemplate.getBucketSchema(bucketName, true);
    List<Long> keys = new ArrayList<Long>();
    for (Object key : (List) schema.get("keys")) {
      keys.add(Long.parseLong(key.toString()));
    }
    return keys;
  }

  public PersistentEntity getIndexedEntity() {
    return association.getAssociatedEntity();
  }

}
