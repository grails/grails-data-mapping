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
package org.springframework.datastore.mock.query

import org.springframework.datastore.query.Query
import org.springframework.datastore.mapping.PersistentEntity
import org.springframework.datastore.mock.SimpleMapSession

/**
 * Simple query implementation that queries a map of objects
 */
class SimpleMapQuery extends Query{

  Map<String, Map> datastore
  private String family

  SimpleMapQuery(SimpleMapSession session, PersistentEntity entity) {
    super(session, entity);
    this.datastore = session.getBackingMap();
    family = getFamily(entity)
  }

  protected List executeQuery(PersistentEntity entity, Query.Junction criteria) {
    if(criteria.isEmpty()) {
      def results = datastore[family]
      if(results) {
        def values = results.values() as List
        return values[offset..max]
      }

    }
    return []; 
  }

  protected String getFamily(PersistentEntity entity) {
      def cm = entity.getMapping()
      String table = null;
      if(cm.getMappedForm() != null) {
          table = cm.getMappedForm().getFamily();
      }
      if(table == null) table = entity.getJavaClass().getName();
      return table;
  }
}
