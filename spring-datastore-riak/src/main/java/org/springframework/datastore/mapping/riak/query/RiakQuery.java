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

package org.springframework.datastore.mapping.riak.query;

import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.query.Query;
import org.springframework.datastore.mapping.riak.collection.RiakEntityIndex;
import org.springframework.datastore.mapping.riak.util.RiakTemplate;

import java.util.List;

/**
 * @author Jon Brisbin <jon.brisbin@npcinternational.com>
 */
public class RiakQuery extends Query {

  private RiakTemplate riakTemplate;

  public RiakQuery(Session session, RiakTemplate riakTemplate, PersistentEntity entity) {
    super(session, entity);
    this.riakTemplate = riakTemplate;
  }

  @Override
  protected List executeQuery(PersistentEntity entity, Junction criteria) {
    final ProjectionList projectionList = projections();
    if (criteria.isEmpty()) {
      // finaAll
      return new RiakEntityIndex(riakTemplate, entity.getName());
    }
    throw new IllegalStateException("Criteria not yet supported");
  }
}
