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

import org.springframework.datastore.core.Session;
import org.springframework.datastore.mapping.PersistentEntity;
import org.springframework.util.Assert;

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
    protected int offset = 0;
    protected Order order = null;
    private Session session;


    /**
     * The ordering of results
     *
     * TODO: Ordering by property name
     */
    public enum Order {
        ASC, DESC
    }

    protected Query(Session session, PersistentEntity entity) {
        this.entity = entity;
        this.session = session;
    }

    public Session getSession() {
        return session;
    }

    public PersistentEntity getEntity() {
        return entity;
    }

    /**
     * Defines the maximum number of results to return
     * @param max The max results
     * @return This query instance
     */
    public Query max(int max) {
        this.max = max;
        return this;
    }

    /**
     * Defines the offset (the first result index) of the query
     * @param offset The offset
     * @return This query instance
     */
    public Query offset(int offset) {
        this.offset = offset;
        return this;
    }

    public Query order(Order order) {
        this.order = order;
        return this;
    }

    /**
     * Restricts the results by the given properties value
     *
     * @param property The name of the property
     * @param value The value to restrict by
     * @return This query instance
     */
    public Query eq(String property, Object value) {
        criteria.add(Restrictions.eq(property, value));
        return this;
    }

    /**
     * Creates a conjunction using two specified criterion
     *
     * @param a The left hand side
     * @param b The right hand side
     * @return This query instance
     */
    public Query and( Criterion a, Criterion b) {
        Assert.notNull(a, "Left hand side of AND cannot be null");
        Assert.notNull(b, "Right hand side of AND cannot be null");
        criteria.add(Restrictions.and(a, b));
        return this;
    }

    /**
     * Creates a disjunction using two specified criterion
     *
     * @param a The left hand side
     * @param b The right hand side
     * @return This query instance
     */
    public Query or( Criterion a, Criterion b) {
        Assert.notNull(a, "Left hand side of AND cannot be null");
        Assert.notNull(b, "Right hand side of AND cannot be null");
        criteria.add(Restrictions.or(a, b));
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
    public static abstract class Criterion {

    }

    /**
     * A criterion that restricts the results based on equality
     */
    public static class Equals extends Criterion {
        private String name;
        private Object value;

        protected Equals(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public Object getValue() {
            return value;
        }
    }

    /**
     * A Criterion used to combine to criterion in a logical AND
     */
    public static class Conjunction extends Criterion {
        private Criterion left;
        private Criterion right;

        protected Conjunction(Criterion left, Criterion right) {
            this.left = left;
            this.right = right;
        }

        public Criterion getLeftSide() {
            return left;
        }

        public Criterion getRightSide() {
            return right;
        }
    }

    /**
     * A Criterion used to combine to criterion in a logical OR
     */
    public static class Disjunction extends Criterion {
        private Criterion left;
        private Criterion right;

        protected Disjunction(Criterion left, Criterion right) {
            this.left = left;
            this.right = right;
        }

        public Criterion getLeftSide() {
            return left;
        }

        public Criterion getRightSide() {
            return right;
        }
    }

}
