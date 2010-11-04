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
package org.springframework.datastore.mapping.query;

import org.springframework.datastore.mapping.core.AbstractDatastore;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.engine.EntityPersister;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.util.Assert;

import javax.persistence.FlushModeType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Models a query that can be executed against a data store
 * 
 * @author Graeme Rocher
 * @since 1.0
 */
public abstract class Query {

    protected PersistentEntity entity;
    protected Junction criteria = new Conjunction();
    protected ProjectionList projections = new ProjectionList();
    protected int max = -1;
    protected int offset = 0;
    protected List<Order> orderBy = new ArrayList<Order>();
    protected Session session;
    protected boolean uniqueResult;


    /**
     * The ordering of results
     *
     */
    public static class Order {
        private Direction direction = Direction.ASC;
        private String property;

        public Order(String property) {
            this.property = property;
        }

        public Order(String property, Direction direction) {
            this.direction = direction;
            this.property = property;
        }

        public Direction getDirection() {
            return direction;
        }

        public String getProperty() {
            return property;
        }

        public static Order desc(String property) {
            return new Order(property, Direction.DESC);
        }
        
        public static Order asc(String property) {
            return new Order(property, Direction.ASC);
        }        

        public static enum Direction {
            ASC, DESC
        }
    }

    protected Query(Session session, PersistentEntity entity) {
        this.entity = entity;
        this.session = session;
    }


    public ProjectionList projections() {
        return projections;
    }

    public void add(Criterion criterion) {
        if(criterion instanceof Equals) {
            final Equals eq = (Equals) criterion;
            eq.setValue(resolveIdIfEntity(eq.getValue()));
        }
        criteria.add(criterion);
    }
    
    public Session getSession() {
        return session;
    }

    public PersistentEntity getEntity() {
        return entity;
    }

    /**
     * Creates a disjunction (OR) query
     * @return The Junction instance
     */
    public Junction disjunction() {
        Disjunction dis = new Disjunction();
        if(criteria.isEmpty()) {
            criteria = dis;
            return criteria;
        }
        else {
            criteria.add(dis);
            return dis;
        }
    }

    /**
     * Creates a negation of several criterion
     * @return The negation
     */
    public Junction negation() {
        Negation dis = new Negation();
        if(criteria.isEmpty()) {
            criteria = dis;
            return criteria;
        }
        else {
            criteria.add(dis);
            return dis;
        }
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
     * Defines the maximum number of results to return
     * @param max The max results
     * @return This query instance
     */
    public Query maxResults(int max) {
        return max(max);
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

    /**
     * Defines the offset (the first result index) of the query
     * @param offset The offset
     * @return This query instance
     */
    public Query firstResult(int offset) {
        return offset(offset);
    }

    /**
     * Specifies the order of results
     * @param order The order object
     * @return The Query instance
     */
    public Query order(Order order) {
        if(order != null)
            this.orderBy.add(order);
        return this;
    }

    /**
     * Gets the Order entries for this query
     * @return The order entries
     */
    public List<Order> getOrderBy() {
        return orderBy;
    }

    /**
     * Restricts the results by the given properties value
     *
     * @param property The name of the property
     * @param value The value to restrict by
     * @return This query instance
     */
    public Query eq(String property, Object value) {
        value = resolveIdIfEntity(value);

        criteria.add(Restrictions.eq(property, value));
        return this;
    }

    private Object resolveIdIfEntity(Object value) {
        // use the object id as the value if its a persistent entity
        if(session.getMappingContext().isPersistentEntity(value)) {
           EntityPersister ep = (EntityPersister) session.getPersister(value);
           value = ep.getObjectIdentifier(value);
        }
        return value;
    }

    /**
     * Used to restrict a value to be greater than the given value
     *
     * @param property The name of the property
     * @param value The value to restrict by
     * @return This query instance
     */
    public Query gt(String property, Object value) {
        criteria.add(Restrictions.gt(property, value));
        return this;
    }    
    
    /**
     * Used to restrict a value to be greater than or equal to the given value
     *
     * @param property The name of the property
     * @param value The value to restrict by
     * @return This query instance
     */
    public Query gte(String property, Object value) {
        criteria.add(Restrictions.gte(property, value));
        return this;
    } 
    
    /**
     * Used to restrict a value to be less than or equal to the given value
     *
     * @param property The name of the property
     * @param value The value to restrict by
     * @return This query instance
     */
    public Query lte(String property, Object value) {
        criteria.add(Restrictions.lte(property, value));
        return this;
    }     
    
    /**
     * Used to restrict a value to be less than the given value
     *
     * @param property The name of the property
     * @param value The value to restrict by
     * @return This query instance
     */
    public Query lt(String property, Object value) {
        criteria.add(Restrictions.lt(property, value));
        return this;
    }    

    /**
     * Restricts the results by the given property values
     *
     * @param property The name of the property
     * @param values The values to restrict by
     * @return This query instance
     */
    public Query in(String property, List values) {
        criteria.add(Restrictions.in(property, values));
        return this;
    }

    /**
     * Restricts the results by the given property value range
     *
     * @param property The name of the property
     * @param start The start of the range
     * @param end The end of the range
     * @return This query instance
     */
    public Query between(String property, Object start, Object end) {
        criteria.add(Restrictions.between(property, start, end));
        return this;
    }
    
    /**
     * Restricts the results by the given properties value
     *
     * @param property The name of the property
     * @param expr The expression to restrict by
     * @return This query instance
     */
    public Query like(String property, String expr) {
        criteria.add(Restrictions.like(property, expr));
        return this;
    }

    /**
     * Restricts the results by the given properties value
     *
     * @param property The name of the property
     * @param expr The expression to restrict by
     * @return This query instance
     */
    public Query rlike(String property, String expr) {
        criteria.add(Restrictions.rlike(property, expr));
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
        uniqueResult = false;
        // flush before query execution in FlushModeType.AUTO
        if(session.getFlushMode() == FlushModeType.AUTO) {
            session.flush();
        }

        return executeQuery(entity, criteria);
    }

    /**
     * Executes the query returning a single result or null
     * @return The result
     */
    public Object singleResult() {
        uniqueResult = true;
        List results = list();
        if(results.isEmpty()) return null;
        else return results.get(0);
    }

    /**
     * Here purely for compatibility
     *
     * @param uniqueResult Whether it is a unique result
     * @deprecated
     */
    public void setUniqueResult(boolean uniqueResult) {
        this.uniqueResult = uniqueResult;
    }

    /**
     * Subclasses should implement this to provide the concrete implementation
     * of querying
     *
     * @param entity The entity
     * @param criteria The criteria
     * @return The results
     */
    protected abstract List executeQuery(PersistentEntity entity, Junction criteria);

    /**
     * A criterion is used to restrict the results of a query
     */
    public static abstract class Criterion {

    }

    /**
     * Criterion that applies to a property
     */
    public static class PropertyCriterion extends Criterion {
        protected String name;
        protected Object value;

        public PropertyCriterion(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        public String getProperty() {
            return name;
        }

        public Object getValue() {
            return value;
        }        
    }
    /**
     * A criterion that restricts the results based on equality
     */
    public static class Equals extends PropertyCriterion {

        public Equals(String name, Object value) {
            super(name, value);
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }

    /**
     * A criterion that restricts the results based on equality
     */
    public static class NotEquals extends PropertyCriterion {

        public NotEquals(String name, Object value) {
            super(name, value);
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }

    /**
     * Criterion used to restrict the results based on a list of values
     */
    public static class In extends Criterion {
        private String name;
        private Collection values = Collections.emptyList();

        public In(String name, Collection values) {
            this.name = name;
            this.values = values;
        }

        public String getName() {
            return name;
        }

        public String getProperty() {
            return getName();
        }

        public Collection getValues() {
            return Collections.unmodifiableCollection(values);
        }
    }

    /**
     * Used to restrict a value to be greater than the given value
     */
    public static class GreaterThan extends PropertyCriterion {
        public GreaterThan(String name, Object value) {
            super(name, value);
        }
    }

    /**
     * Used to restrict a value to be greater than or equal to the given value
     */
    public static class GreaterThanEquals extends PropertyCriterion {
        public GreaterThanEquals(String name, Object value) {
            super(name, value);
        }
    }
    
    /**
     * Used to restrict a value to be less than the given value
     */
    public static class LessThan extends PropertyCriterion {
        public LessThan(String name, Object value) {
            super(name, value);
        }
    }    
    
    /**
     * Used to restrict a value to be less than the given value
     */
    public static class LessThanEquals extends PropertyCriterion {
        public LessThanEquals(String name, Object value) {
            super(name, value);
        }
    }    
    /**
     * Criterion used to restrict the result to be between values (range query)
     */
    public static class Between extends Criterion {
        private String property;
        private Object from; 
        private Object to;

        public Between(String property, Object from, Object to) {
            this.property = property;
            this.from = from;
            this.to = to;
        }

        public String getProperty() {
            return property;
        }

        public Object getFrom() {
            return from;
        }

        public Object getTo() {
            return to;
        }
    }
    
    /**
     * Criterion used to restrict the results based on a pattern (likeness)
     */
    public static class Like extends PropertyCriterion {
        public Like(String name, String expression) {
            super(name, expression);
        }

        public String getPattern() {
            return getValue().toString();
        }
    }

    /**
     * Criterion used to restrict the results based on a regular expression pattern
     */
    public static class RLike extends Like {
        public RLike(String name, String expression) {
            super(name, expression);
        }

        public String getPattern() {
            return getValue().toString();
        }
    }

    public static abstract class Junction extends Criterion {
        private List<Criterion> criteria = new ArrayList<Criterion>();

        public Junction add(Criterion c) {
            if(c != null) {
                if(c instanceof Equals) {
                    final Equals eq = (Equals) c;
                    Object value = eq.getValue();
                    Session session = AbstractDatastore.retrieveSession();
                    if(session.getMappingContext().isPersistentEntity(value)) {
                        EntityPersister ep = (EntityPersister) session.getPersister(value);
                        c = new Equals(eq.getProperty(), ep.getObjectIdentifier(value));
                    }
                }
                criteria.add(c);
            }
            return this;
        }

        public List<Criterion> getCriteria() {
            return criteria;
        }

        public boolean isEmpty() {
            return criteria.isEmpty();
        }
    }
    /**
     * A Criterion used to combine to criterion in a logical AND
     */
    public static class Conjunction extends Junction {
    }

    /**
     * A Criterion used to combine to criterion in a logical OR
     */
    public static class Disjunction extends Junction {
    }

    /**
     * A criterion used to negate several other criterion
     */
    public static class Negation extends Junction {}

    /**
     * A projection
     */
    public static class Projection {}

    /**
     * A projection used to obtain the identifier of an object
     */
    public static class IdProjection extends Projection {}

    /**
     * Used to count the results of a query
     */
    public static class CountProjection extends Projection {}

    /**
     * A projection that obtains the value of a property of an entity
     */
    public static class PropertyProjection extends Projection {
        private String propertyName;

        protected PropertyProjection(String propertyName) {
            this.propertyName = propertyName;
        }

        public String getPropertyName() {
            return propertyName;
        }
    }
    /**
     * Computes the average value of a property
     */
    public static class AvgProjection extends PropertyProjection {
        protected AvgProjection(String propertyName) {
            super(propertyName);
        }
    }
    
    /**
     * Computes the max value of a property
     */
    public static class MaxProjection extends PropertyProjection {
        protected MaxProjection(String propertyName) {
            super(propertyName);
        }
    }
    
    /**
     * Computes the min value of a property
     */
    public static class MinProjection extends PropertyProjection {
        protected MinProjection(String propertyName) {
            super(propertyName);
        }
    }
    
    /**
     * Computes the sum of a property
     */
    public static class SumProjection extends PropertyProjection {
        protected SumProjection(String propertyName) {
            super(propertyName);
        }
    }
    

    /**
     * A list of projections
     */
    public static class ProjectionList {

        private List<Projection> projections = new ArrayList();

        public List<Projection> getProjectionList() {
            return Collections.unmodifiableList(projections);
        }

        public ProjectionList add(Projection p) {
            projections.add(p);
            return this;
        }

        public ProjectionList id() {
            add(Projections.id());
            return this;
        }

        public ProjectionList count() {
            add(Projections.count());
            return this;
        }

        public boolean isEmpty() {
            return projections.isEmpty();
        }
        
        /**
         * A projection that obtains the value of a property of an entity
         * @param name The name of the property
         * @return The PropertyProjection instance
         */
        public ProjectionList property(String name) {
            add(Projections.property(name));
            return this;
        }
    
    
        /**
         * Computes the sum of a property
         *
         * @param name The name of the property
         * @return The PropertyProjection instance
         */
        public ProjectionList sum(String name) {
            add(Projections.sum(name));
            return this;
        }
    
        /**
         * Computes the min value of a property
         *
         * @param name The name of the property
         * @return The PropertyProjection instance
         */
        public ProjectionList min(String name) {
            add(Projections.min(name));
            return this;
        }
    
        /**
         * Computes the max value of a property
         *
         * @param name The name of the property
         * @return The PropertyProjection instance
         */
        public ProjectionList max(String name) {
            add(Projections.max(name));
            return this;
        }
    
       /**
         * Computes the average value of a property
         *
         * @param name The name of the property
         * @return The PropertyProjection instance
         */
        public ProjectionList avg(String name) {
           add(Projections.avg(name));
            return this;
        }        
    }


}
