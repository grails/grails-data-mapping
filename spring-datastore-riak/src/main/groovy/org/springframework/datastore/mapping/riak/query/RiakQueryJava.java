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
import org.springframework.datastore.riak.core.RiakTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jon Brisbin <jon.brisbin@npcinternational.com>
 */
public class RiakQueryJava extends Query {

  private static final Map<Class, QueryHandler> queryHandlers = new LinkedHashMap<Class, QueryHandler>();

  static {
    queryHandlers.put(Equals.class, new QueryHandler<Equals>() {
      public void handle(PersistentEntity entity, Equals criterion) {
        System.out.println("entity: " + entity);
      }
    });
  }

  private RiakTemplate riakTemplate;

  public RiakQueryJava(Session session, RiakTemplate riakTemplate, PersistentEntity entity) {
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
    for (Criterion cr : criteria.getCriteria()) {
      queryHandlers.get(cr.getClass()).handle(entity, cr);
    }
    return new ArrayList();
  }

  private static interface QueryHandler<T> {
    public void handle(PersistentEntity entity, T criterion);
  }
}
