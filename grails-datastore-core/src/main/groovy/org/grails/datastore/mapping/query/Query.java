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
package org.grails.datastore.mapping.query;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.persistence.FlushModeType;

import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.grails.datastore.mapping.core.AbstractDatastore;
import org.grails.datastore.mapping.core.Session;
import org.grails.datastore.mapping.core.SessionImplementor;
import org.grails.datastore.mapping.engine.EntityAccess;
import org.grails.datastore.mapping.engine.EntityPersister;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.types.Association;
import org.springframework.util.Assert;

/**
 * Models a query that can be executed against a data store
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings({"hiding", "rawtypes", "unchecked"})
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
     * @return The criteria defined by this query
     */
    public Junction getCriteria() {
        return criteria;
    }

    /**
     * The ordering of results.
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

    /**
     * Adds the specified criterion instance to the query
     *
     * @param criterion The criterion instance
     */
    public void add(Criterion criterion) {
        if (criterion instanceof Equals) {
            final Equals eq = (Equals) criterion;
            Object value = resolveIdIfEntity(eq.getValue());
        }

        criteria.add(criterion);
    }

    /**
     * @return The session that created the query
     */
    public Session getSession() {
        return session;
    }

    /**
     * @return The PersistentEntity being query
     */
    public PersistentEntity getEntity() {
        return entity;
    }

    /**
     * Creates a disjunction (OR) query
     * @return The Junction instance
     */
    public Junction disjunction() {
        Disjunction dis = new Disjunction();
        if (criteria.isEmpty()) {
            criteria = dis;
            return criteria;
        }
        criteria.add(dis);
        return dis;
    }

    /**
     * Creates a negation of several criterion
     * @return The negation
     */
    public Junction negation() {
        Negation dis = new Negation();
        if (criteria.isEmpty()) {
            criteria = dis;
            return criteria;
        }
        criteria.add(dis);
        return dis;
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
        if (order != null) {
            orderBy.add(order);
        }
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
        Object resolved = resolveIdIfEntity(value);
        if (resolved == value) {
           criteria.add(Restrictions.eq(property, value));
        }
        else {
           criteria.add(Restrictions.eq(property,resolved));
        }
        return this;
    }

    /**
     * Shortcut to restrict the query to multiple given property values
     *
     * @param values The values
     * @return This query instance
     */
    public Query allEq(Map<String, Object> values) {
        for (String property : values.keySet()) {
            eq(property, values.get(property));
        }
        return this;
    }

    /**
     * Used to restrict a value to be empty (such as a blank string or an empty collection)
     *
     * @param property The property name
    */
    public Query isEmpty(String property) {
        criteria.add(Restrictions.isEmpty(property));
        return this;
    }

    /**
     * Used to restrict a value to be not empty (such as a blank string or an empty collection)
     *
     * @param property The property name
    */
    public Query isNotEmpty(String property) {
        criteria.add(Restrictions.isNotEmpty(property));
        return this;
    }

    /**
     * Used to restrict a property to be null
     *
     * @param property The property name
    */
    public Query isNull(String property) {
        criteria.add(Restrictions.isNull(property));
        return this;
    }

    /**
     * Used to restrict a property to be not null
     *
     * @param property The property name
    */
    public Query isNotNull(String property) {
        criteria.add(Restrictions.isNotNull(property));
        return this;
    }

    /**
     * Creates an association query
     *
     * @param associationName The assocation name
     * @return The Query instance
     */
    public Query createQuery(String associationName) {
        final PersistentProperty property = entity.getPropertyByName(associationName);
        if (property == null || !(property instanceof Association)) {
            throw new InvalidDataAccessResourceUsageException("Cannot query association [" +
                  associationName + "] of class [" + entity +
                  "]. The specified property is not an association.");
        }

        Association association = (Association) property;

        final PersistentEntity associatedEntity = association.getAssociatedEntity();

        final AssociationQuery associationQuery = new AssociationQuery(session, associatedEntity, association);
        add(associationQuery);
        return associationQuery;
    }

    /**
     * Restricts the results by the given properties value
     *
     * @param value The value to restrict by
     * @return This query instance
     */
    public Query idEq(Object value) {
        value = resolveIdIfEntity(value);

        criteria.add(Restrictions.idEq(value));
        return this;
    }

    protected Object resolveIdIfEntity(Object value) {
        // use the object id as the value if its a persistent entity
        return session.getMappingContext().isPersistentEntity(value) ? findInstanceId(value) : value;
    }

    private Serializable findInstanceId(Object value) {
        EntityPersister ep = (EntityPersister) session.getPersister(value);
        if (ep != null) {
            return ep.getObjectIdentifier(value);
        }

        return (Serializable)new EntityAccess(session
                .getMappingContext()
                .getPersistentEntity(value.getClass().getName()), value)
                .getIdentifier();
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
     * Used to restrict a value to be greater than or equal to the given value
     *
     * @param property The name of the property
     * @param value The value to restrict by
     * @return This query instance
     */
    public Query ge(String property, Object value) {
        return gte(property, value);
    }

    /**
     * Used to restrict a value to be less than or equal to the given value
     *
     * @param property The name of the property
     * @param value The value to restrict by
     * @return This query instance
     */
    public Query le(String property, Object value) {
        return lte(property, value);
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
    public Query ilike(String property, String expr) {
        criteria.add(Restrictions.ilike(property, expr));
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
     * Executes the query returning zero or many results as a list.
     *
     * @return The results
     */
    public List list() {
        uniqueResult = false;
        flushBeforeQuery();

        List results = executeQuery(entity, criteria);

        if (session instanceof SessionImplementor) {
            SessionImplementor sessionImplementor = (SessionImplementor)session;
            for (ListIterator iter = results.listIterator(); iter.hasNext(); ) {
                Object instance = iter.next();
                EntityPersister ep = (EntityPersister) session.getPersister(instance);
                if (ep == null) {
                    // not persistent, could be a count() or report query
                    continue;
                }

                Serializable id = findInstanceId(instance);
                if (sessionImplementor.isCached(instance.getClass(), id)) {
                    iter.set(sessionImplementor.getCachedInstance(instance.getClass(), id));
                }
                else {
                    sessionImplementor.cacheInstance(instance.getClass(), id, instance);
                }
            }
        }

        return results;
    }

    /**
     * Default behavior is the flush the session before a query in the case of FlushModeType.AUTO.
     * Subclasses can override this method to disable that.
     */
    protected void flushBeforeQuery() {
        // flush before query execution in FlushModeType.AUTO
        if (session.getFlushMode() == FlushModeType.AUTO) {
            session.flush();
        }
    }

    /**
     * Executes the query returning a single result or null
     * @return The result
     */
    public Object singleResult() {
        uniqueResult = true;
        List results = list();
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Here purely for compatibility
     *
     * @param uniqueResult Whether it is a unique result
     * @deprecated
     */
    @Deprecated
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
    public static interface Criterion {

    }

    /**
     * Restricts a property to be null
     */
    public static class IsNull extends PropertyNameCriterion {
        public IsNull(String name) {
            super(name);
        }
    }

    /**
     * Restricts a property to be empty (such as a blank string)
     */
    public static class IsEmpty extends PropertyNameCriterion {
        public IsEmpty(String name) {
            super(name);
        }
    }

    /**
     * Restricts a property to be empty (such as a blank string)
     */
    public static class IsNotEmpty extends PropertyNameCriterion {
        public IsNotEmpty(String name) {
            super(name);
        }
    }

    /**
     * Restricts a property to be not null
     */
    public static class IsNotNull extends PropertyNameCriterion {
        public IsNotNull(String name) {
            super(name);
        }
    }

    /**
     * A Criterion that applies to a property
     */
    public static class PropertyNameCriterion implements Criterion {
        protected String name;

        public PropertyNameCriterion(String name) {
            this.name = name;
        }

        public String getProperty() {
            return name;
        }
    }

    /**
     * A Criterion that compares to properties
     */
    public static class PropertyComparisonCriterion implements Criterion {
        protected String name;
        protected String otherProperty;

        public PropertyComparisonCriterion(String property, String otherProperty) {
            this.name = name;
            this.otherProperty = otherProperty;
        }

        public String getProperty() {
            return name;
        }

        public String getOtherProperty() {
            return otherProperty;
        }
    }

    public static class EqualsProperty extends PropertyComparisonCriterion {
        public EqualsProperty(String property, String otherProperty) {
            super(property, otherProperty);
        }
    }

    public static class NotEqualsProperty extends PropertyComparisonCriterion {
        public NotEqualsProperty(String property, String otherProperty) {
            super(property, otherProperty);
        }
    }

    public static class GreaterThanProperty extends PropertyComparisonCriterion {
        public GreaterThanProperty(String property, String otherProperty) {
            super(property, otherProperty);
        }
    }

    public static class GreaterThanEqualsProperty extends PropertyComparisonCriterion {
        public GreaterThanEqualsProperty(String property, String otherProperty) {
            super(property, otherProperty);
        }
    }

    public static class LessThanProperty extends PropertyComparisonCriterion {
        public LessThanProperty(String property, String otherProperty) {
            super(property, otherProperty);
        }
    }

    public static class LessThanEqualsProperty extends PropertyComparisonCriterion {
        public LessThanEqualsProperty(String property, String otherProperty) {
            super(property, otherProperty);
        }
    }

    /**
     * Criterion that applies to a property and value
     */
    public static class PropertyCriterion extends PropertyNameCriterion {

        protected Object value;

        public PropertyCriterion(String name, Object value) {
            super(name);
            this.value = value;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object v) {
            this.value = v;
        }
    }
    /**
     * A criterion that restricts the results based on equality
     */
    public static class Equals extends PropertyCriterion {

        public Equals(String name, Object value) {
            super(name, value);
        }

        @Override
        public void setValue(Object value) {
            this.value = value;
        }
    }

    public static class SizeEquals extends PropertyCriterion{
        public SizeEquals(String name, int value) {
            super(name, value);
        }
    }

    public static class SizeNotEquals extends PropertyCriterion{
        public SizeNotEquals(String name, int value) {
            super(name, value);
        }
    }

    public static class SizeGreaterThan extends PropertyCriterion{
        public SizeGreaterThan(String name, int value) {
            super(name, value);
        }
    }

    public static class SizeGreaterThanEquals extends PropertyCriterion{
        public SizeGreaterThanEquals(String name, int value) {
            super(name, value);
        }
    }

    public static class SizeLessThanEquals extends PropertyCriterion{
        public SizeLessThanEquals(String name, int value) {
            super(name, value);
        }
    }

    public static class SizeLessThan extends PropertyCriterion{
        public SizeLessThan(String name, int value) {
            super(name, value);
        }
    }


    /**
     * A criterion that restricts the results based on the equality of the identifier
     */
    public static class IdEquals extends PropertyCriterion {

        private static final String ID = "id";

        public IdEquals(Object value) {
            super(ID, value);
        }

        @Override
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

        @Override
        public void setValue(Object value) {
            this.value = value;
        }
    }

    /**
     * Criterion used to restrict the results based on a list of values
     */
    public static class In extends PropertyCriterion {
        private String name;
        private Collection values = Collections.emptyList();

        public In(String name, Collection values) {
            super(name, values);
            this.name = name;
            this.values = values;
        }

        public String getName() {
            return name;
        }

        @Override
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
    public static class Between extends PropertyCriterion {
        private String property;
        private Object from;
        private Object to;

        public Between(String property, Object from, Object to) {
            super(property, from);
            this.property = property;
            this.from = from;
            this.to = to;
        }

        @Override
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
     * Criterion used to restrict the results based on a pattern (likeness)
     */
    public static class ILike extends Like {
        public ILike(String name, String expression) {
            super(name, expression);
        }
    }

    /**
     * Criterion used to restrict the results based on a regular expression pattern
     */
    public static class RLike extends Like {
        public RLike(String name, String expression) {
            super(name, expression);
        }

        @Override
        public String getPattern() {
            return getValue().toString();
        }
    }

    public static abstract class Junction implements Criterion {
        private List<Criterion> criteria = new ArrayList<Criterion>();

        public Junction add(Criterion c) {
            if (c != null) {
                if (c instanceof Equals) {
                    final Equals eq = (Equals) c;
                    Object value = eq.getValue();

                    if (value != null) {

                        Session session = AbstractDatastore.retrieveSession();
                        final PersistentEntity persistentEntity = session.getMappingContext().getPersistentEntity(value.getClass().getName());
                        if (persistentEntity != null) {
                            EntityPersister ep = (EntityPersister) session.getPersister(value);

                            if (ep != null) {
                                c = new Equals(eq.getProperty(), ep.getObjectIdentifier(value));
                            }
                        }
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
    public static class Conjunction extends Junction {}

    /**
     * A Criterion used to combine to criterion in a logical OR
     */
    public static class Disjunction extends Junction {}

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
    
    public static class DistinctProjection extends Projection {}
    

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

    public static class DistinctPropertyProjection extends PropertyProjection{
        protected DistinctPropertyProjection(String propertyName) {
            super(propertyName);
        }
    }

    public static class CountDistinctProjection extends PropertyProjection{
        public CountDistinctProjection(String property) {
            super(property);
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
    public static class ProjectionList implements org.grails.datastore.mapping.query.api.Projections{

        private List<Projection> projections = new ArrayList();

        public List<Projection> getProjectionList() {
            return Collections.unmodifiableList(projections);
        }

        public ProjectionList add(Projection p) {
            projections.add(p);
            return this;
        }

        public org.grails.datastore.mapping.query.api.Projections id() {
            add(Projections.id());
            return this;
        }

        public org.grails.datastore.mapping.query.api.Projections count() {
            add(Projections.count());
            return this;
        }

        public org.grails.datastore.mapping.query.api.Projections countDistinct(String property) {
            add(Projections.countDistinct(property));
            return this;
        }

        public boolean isEmpty() {
            return projections.isEmpty();
        }
        
        public ProjectionList distinct() {
        	return this;
        }

        public org.grails.datastore.mapping.query.api.Projections distinct(String property) {
            add(Projections.distinct(property));
            return this;
        }

        public org.grails.datastore.mapping.query.api.Projections rowCount() {
            return count();
        }

        /**
         * A projection that obtains the value of a property of an entity
         * @param name The name of the property
         * @return The PropertyProjection instance
         */
        public org.grails.datastore.mapping.query.api.Projections property(String name) {
            add(Projections.property(name));
            return this;
        }

        /**
         * Computes the sum of a property
         *
         * @param name The name of the property
         * @return The PropertyProjection instance
         */
        public org.grails.datastore.mapping.query.api.Projections sum(String name) {
            add(Projections.sum(name));
            return this;
        }

        /**
         * Computes the min value of a property
         *
         * @param name The name of the property
         * @return The PropertyProjection instance
         */
        public org.grails.datastore.mapping.query.api.Projections min(String name) {
            add(Projections.min(name));
            return this;
        }

        /**
         * Computes the max value of a property
         *
         * @param name The name of the property
         * @return The PropertyProjection instance
         */
        public org.grails.datastore.mapping.query.api.Projections max(String name) {
            add(Projections.max(name));
            return this;
        }

        /**
         * Computes the average value of a property
         *
         * @param name The name of the property
         * @return The PropertyProjection instance
         */
        public org.grails.datastore.mapping.query.api.Projections avg(String name) {
            add(Projections.avg(name));
            return this;
        }
    }

}
