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
package org.springframework.datastore.query;

import org.springframework.datastore.mapping.PersistentEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Models a query that can be executed against a data store
 * 
 * @author Graeme Rocher
 * @since 1.0
 */
public abstract class Query {

    protected PersistentEntity entity;
    protected List<Criterion> criteria = new ArrayList<Criterion>();
    protected int max = -1;
    protected int offset = -1;
    protected Order order = null;


    /**
     * The ordering of results
     *
     * TODO: Ordering by property name
     */
    public enum Order {
        ASC, DESC
    }

    protected Query(PersistentEntity entity) {
        this.entity = entity;
    }

    public PersistentEntity getEntity() {
        return entity;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    /**
     * Restricts the results by the given properties value
     *
     * @param property The name of the property
     * @param value The value to restrict by
     * @return This query instance
     */
    public Query eq(String property, Object value) {
        criteria.add(new Equals(this, property, value));
        return this;
    }

    /**
     * Executes the query returning zero or many results as a list
     *
     * @return The results
     */
    public List list() {
        return executeQuery(entity, criteria);
    }

    /**
     * Subclasses should implement this to provide the concrete implementation
     * of querying
     *
     * @param entity The entity
     * @param criteria The criteria
     * @return The results
     */
    protected abstract List executeQuery(PersistentEntity entity, List<Criterion> criteria);

    /**
     * A criterion is used to restrict the results of a query
     */
    public abstract class Criterion {

        private Query query;

        protected Criterion(Query query) {
            this.query = query;
        }

        public Query getQuery() {
            return query;
        }
    }

    /**
     * A criterion that reststricts the results based on equality
     */
    public class Equals extends Criterion {
        private String name;
        private Object value;

        protected Equals(Query query, String name, Object value) {
            super(query);
            this.name = name;
            this.value = value;
        }
    }
}
