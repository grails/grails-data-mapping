/* Copyright (C) 2011 SpringSource
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

package grails.gorm

import org.grails.datastore.mapping.query.api.Criteria
import org.grails.datastore.mapping.query.api.ProjectionList

import org.grails.datastore.mapping.query.Query.Criterion
import org.grails.datastore.mapping.query.Restrictions
import org.grails.datastore.mapping.query.Query.Order
import org.grails.datastore.mapping.query.Query.Projection
import org.grails.datastore.mapping.query.Projections
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.query.Query
import org.grails.datastore.gorm.finders.DynamicFinder
import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.mapping.query.Query.Junction
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.gorm.query.criteria.DetachedAssociationCriteria
import org.grails.datastore.mapping.query.Query.Order.Direction
import org.grails.datastore.mapping.query.api.QueryableCriteria
import org.grails.datastore.mapping.query.Query.PropertyCriterion

/**
 * Represents criteria that is not bound to the current connection and can be built up and re-used at a later date
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class DetachedCriteria<T> implements QueryableCriteria<T>, Cloneable, Iterable<T> {
    
    private List<Criterion> criteria = []
    private List<Order> orders = []
    private List<Projection> projections = []
    private Class targetClass
    private List<DynamicFinder> dynamicFinders = null
    private Integer defaultOffset = null
    private Integer defaultMax = null

    private List<Junction> junctions = []
    private PersistentEntity persistentEntity
    ProjectionList projectionList = new DetachedProjections(projections)

    /**
     * Constructs a DetachedCriteria instance target the given class
     * @param targetClass
     */
    DetachedCriteria(Class<T> targetClass) {
        this.targetClass = targetClass
    }

    PersistentEntity getPersistentEntity() {
        if(persistentEntity == null) initialiseIfNecessary(targetClass)
        return persistentEntity
    }

    protected def initialiseIfNecessary(Class<T> targetClass) {
        if(dynamicFinders == null) {
            try {
                dynamicFinders = targetClass.gormDynamicFinders
                persistentEntity = targetClass.gormPersistentEntity
            } catch (MissingPropertyException mpe) {
                throw new IllegalArgumentException("Class [$targetClass.name] is not a domain class")
            }
        }
    }

    public void add(Criterion criterion) {
        if(criterion instanceof PropertyCriterion) {
            if(criterion.value instanceof Closure) {
                criterion.value = buildQueryableCriteria(criterion.value)
            }
        }
        if(junctions)  {
            junctions[-1].add criterion
        }
        else {
            criteria << criterion
        }
    }

    public List<Criterion> getCriteria() { criteria }

    public List<Projection> getProjections() { projections }

    public List<Order> getOrders() { orders }

    /**
     * Evaluate projections within the context of the given closure
     *
     * @param callable The callable
     * @return  The projection list
     */
    ProjectionList projections(Closure callable) {
        callable.delegate = projectionList
        callable.call()
        return projectionList
    }

    /**
     * Handles a conjunction
     * @param callable Callable closure
     * @return This criterion
     */
    Criteria and(Closure callable) {
        junctions << new Query.Conjunction()
        handleJunction(callable)
        return this
    }


    /**
     * Handles a disjunction
     * @param callable Callable closure
     * @return This criterion
     */
    Criteria or(Closure callable) {
        junctions << new Query.Disjunction()
        handleJunction(callable)
        return this
    }

    /**
     * Handles a disjunction
     * @param callable Callable closure
     * @return This criterion
     */
    Criteria not(Closure callable) {
        junctions << new Query.Negation()
        handleJunction(callable)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria 'in'(String propertyName, Collection values) {
        inList(propertyName, values)
    }

    /**
     * @see Criteria
     */
    Criteria 'in'(String propertyName, Object[] values) {
        inList(propertyName, values)
    }

    /**
     * @see Criteria
     */
    Criteria order(String propertyName) {
        orders << new Order(propertyName)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria order(String propertyName, String direction) {
        orders << new Order(propertyName, Direction.valueOf(direction.toUpperCase()))
        return this
    }

    /**
     * @see Criteria
     */
    Criteria inList(String propertyName, Collection values) {
        add Restrictions.in(propertyName, values)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria inList(String propertyName, Object[] values) {
        add Restrictions.in(propertyName, Arrays.asList(values))
        return this
    }

    /**
     * @see Criteria
     */
    Criteria sizeEq(String propertyName, int size) {
        add Restrictions.sizeEq(propertyName, size)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria sizeGt(String propertyName, int size) {
        add Restrictions.sizeGt(propertyName, size)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria sizeGe(String propertyName, int size) {
        add Restrictions.sizeGe(propertyName, size)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria sizeLe(String propertyName, int size) {
        add Restrictions.sizeLe(propertyName, size)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria sizeLt(String propertyName, int size) {
        add Restrictions.sizeLt(propertyName, size)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria sizeNe(String propertyName, int size) {
        add Restrictions.sizeNe(propertyName, size)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria eqProperty(String propertyName, String otherPropertyName) {
        add Restrictions.eqProperty(propertyName,otherPropertyName)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria neProperty(String propertyName, String otherPropertyName) {
        add Restrictions.neProperty(propertyName,otherPropertyName)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria gtProperty(String propertyName, String otherPropertyName) {
        add Restrictions.gtProperty(propertyName,otherPropertyName)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria geProperty(String propertyName, String otherPropertyName) {
        add Restrictions.geProperty(propertyName,otherPropertyName)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria ltProperty(String propertyName, String otherPropertyName) {
        add Restrictions.ltProperty(propertyName,otherPropertyName)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria leProperty(String propertyName, String otherPropertyName) {
        add Restrictions.leProperty(propertyName,otherPropertyName)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria idEquals(Object value) {
        add Restrictions.idEq(value)
        return this
    }


    /**
     * @see Criteria
     */
    Criteria isEmpty(String propertyName) {
        add Restrictions.isEmpty(propertyName)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria isNotEmpty(String propertyName) {
        add Restrictions.isNotEmpty(propertyName)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria isNull(String propertyName) {
        add Restrictions.isNull(propertyName)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria isNotNull(String propertyName) {
        add Restrictions.isNotNull(propertyName)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria eq(String propertyName, Object propertyValue) {
        add Restrictions.eq(propertyName,propertyValue)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria idEq(Object propertyValue) {
        add Restrictions.idEq(propertyValue)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria ne(String propertyName, Object propertyValue) {
        add Restrictions.ne(propertyName,propertyValue)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria between(String propertyName, Object start, Object finish) {
        add Restrictions.between(propertyName, start, finish)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria gte(String property, Object value) {
        add Restrictions.gte(property,value)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria ge(String property, Object value) {
        gte(property, value)
    }

    /**
     * @see Criteria
     */
    Criteria gt(String property, Object value) {
        add Restrictions.gt(property,value)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria lte(String property, Object value) {
        add Restrictions.lte(property, value)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria le(String property, Object value) {
        lte(property,value)
    }

    /**
     * @see Criteria
     */
    Criteria lt(String property, Object value) {
        add Restrictions.lt(property,value)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria like(String propertyName, Object propertyValue) {
        add Restrictions.like(propertyName,propertyValue.toString())
        return this
    }

    /**
     * @see Criteria
     */
    Criteria ilike(String propertyName, Object propertyValue) {
        add Restrictions.ilike(propertyName, propertyValue.toString())
        return this
    }

    /**
     * @see Criteria
     */
    Criteria rlike(String propertyName, Object propertyValue) {
        add Restrictions.rlike(propertyName, propertyValue.toString())
        return this
    }

    Criteria eqAll(String propertyName, Closure propertyValue) {
        eqAll(propertyName, buildQueryableCriteria(propertyValue))
    }

    private QueryableCriteria buildQueryableCriteria(Closure queryClosure) {
        return (QueryableCriteria) new DetachedCriteria(targetClass).build(queryClosure);
    }

    Criteria gtAll(String propertyName, Closure propertyValue) {
        gtAll(propertyName, buildQueryableCriteria(propertyValue))
    }

    Criteria ltAll(String propertyName, Closure propertyValue) {
        ltAll(propertyName, buildQueryableCriteria(propertyValue))
    }

    Criteria geAll(String propertyName, Closure propertyValue) {
        geAll(propertyName, buildQueryableCriteria(propertyValue))
    }

    Criteria leAll(String propertyName, Closure propertyValue) {
        leAll(propertyName, buildQueryableCriteria(propertyValue))
    }

    @Override
    Criteria eqAll(String propertyName, QueryableCriteria propertyValue) {
        add new Query.EqualsAll(propertyName, propertyValue)
        return this
    }

    @Override
    Criteria gtAll(String propertyName, QueryableCriteria propertyValue) {
        add new Query.GreaterThanAll(propertyName, propertyValue)
        return this
    }

    @Override
    Criteria ltAll(String propertyName, QueryableCriteria propertyValue) {
        add new Query.LessThanAll(propertyName, propertyValue)
        return this

    }

    @Override
    Criteria geAll(String propertyName, QueryableCriteria propertyValue) {
        add new Query.GreaterThanEqualsAll(propertyName, propertyValue)
        return this

    }

    @Override
    Criteria leAll(String propertyName, QueryableCriteria propertyValue) {
        add new Query.LessThanEqualsAll(propertyName, propertyValue)
        return this
    }

    class DetachedProjections implements ProjectionList {

        List<Projection> projections


        DetachedProjections(List<Projection> projections) {
            this.projections = projections
        }
/**
         * @see ProjectionList
         */
        ProjectionList avg(String name) {
            projections << Projections.avg(name)
            return this
        }

        /**
         * @see ProjectionList
         */
        ProjectionList max(String name) {
            projections << Projections.max(name)
            return this
        }

        /**
         * @see ProjectionList
         */
        ProjectionList min(String name) {
            projections << Projections.min(name)
            return this
        }

        /**
         * @see ProjectionList
         */
        ProjectionList sum(String name) {
            projections << Projections.sum(name)
            return this
        }

        /**
         * @see ProjectionList
         */
        ProjectionList property(String name) {
            projections << Projections.property(name)
            return this
        }

        /**
         * @see ProjectionList
         */
        ProjectionList rowCount() {
            projections << Projections.count()
            return this
        }

        /**
         * @see ProjectionList
         */
        ProjectionList distinct(String property) {
            projections << Projections.distinct(property)
            return this
        }

        /**
         * @see ProjectionList
         */
        ProjectionList distinct() {
            projections << Projections.distinct()
            return this
        }

        /**
         * @see ProjectionList
         */
        ProjectionList countDistinct(String property) {
            projections << Projections.countDistinct(property)
            return this
        }

        /**
         * @see ProjectionList
         */
        ProjectionList count() {
            projections << Projections.count()
            return this
        }

        /**
         * @see ProjectionList
         */
        ProjectionList id() {
            projections << Projections.id()
            return this
        }

    }

    /**
     * Where method derives a new query from this query. This method will not mutate the original query, but instead return a new one.
     *
     * @param additionalQuery The additional query
     * @return A new query
     */
    DetachedCriteria<T> where(Closure additionalQuery) {
        DetachedCriteria<T> newQuery = clone()

        return newQuery.build( additionalQuery )
    }
    /**
     * Synonym for #get
     */
    T find( Map args = Collections.emptyMap(), Closure additionalCriteria = null) {
        get(args, additionalCriteria)
    }

   /**
     * Synonym for #get
     */
    T find( Closure additionalCriteria) {
        get(Collections.emptyMap(), additionalCriteria)
    }

    /**
     * Lists all records matching the criterion contained within this DetachedCriteria instance
     *
     * @return A list of matching instances
     */
    T get( Map args = Collections.emptyMap(), Closure additionalCriteria = null) {
        (T)withPopulatedQuery(args, additionalCriteria) { Query query ->
            query.singleResult()
        }
    }

    /**
     * Lists all records matching the criterion contained within this DetachedCriteria instance
     *
     * @return A list of matching instances
     */
    T get( Closure additionalCriteria) {
        get(Collections.emptyMap(), additionalCriteria)
    }

    /**
     * Lists all records matching the criterion contained within this DetachedCriteria instance
     *
     * @return A list of matching instances
     */
    List<T> list( Map args = Collections.emptyMap(), Closure additionalCriteria = null) {
        (List)withPopulatedQuery(args, additionalCriteria) { Query query ->
            query.list()
        }
    }

    /**
     * Lists all records matching the criterion contained within this DetachedCriteria instance
     *
     * @return A list of matching instances
     */
    List<T> list( Closure additionalCriteria) {
        list(Collections.emptyMap(), additionalCriteria)
    }

    /**
     * Counts the number of records returned by the query
     *
     * @param args The arguments
     * @return The count
     */
    Number count(Map args = Collections.emptyMap(), Closure additionalCriteria = null) {
        (Number)withPopulatedQuery(args, additionalCriteria) { Query query ->
            query.projections().count()
            query.singleResult()
        }
    }


    /**
     * Counts the number of records returned by the query
     *
     * @param args The arguments
     * @return The count
     */
    Number count(Closure additionalCriteria ) {
        (Number)withPopulatedQuery(Collections.emptyMap(), additionalCriteria) { Query query ->
            query.projections().count()
            query.singleResult()
        }
    }

    /**
     * Counts the number of records returned by the query
     *
     * @param args The arguments
     * @return The count
     */
    boolean exists(Closure additionalCriteria ) {
        (Boolean)withPopulatedQuery(Collections.emptyMap(), additionalCriteria) { Query query ->
            query.projections().count()
            query.singleResult() > 0
        }
    }

    /**
     * Deletes all entities matching this criteria
     *
     * @return The total number deleted
     */
    Number deleteAll() {
        targetClass.withDatastoreSession { Session session ->
            session.deleteAll(this)
        }
    }

    /**
     * Updates all entities matching this criteria
     *
     * @return The total number deleted
     */
    Number updateAll(Map properties) {
        targetClass.withDatastoreSession { Session session ->
            session.updateAll(this, properties)
        }
    }

    /**
     * Enable the builder syntax for contructing Criteria
     *
     * @param callable The callable closure
     * @return This criteria instance
     */

    DetachedCriteria<T> build(Closure callable) {
        DetachedCriteria newCriteria = this.clone()
        newCriteria.with callable
        return newCriteria
    }

    /**
     * Sets the default max to use and returns a new criteria instance. This method does not mutate the original criteria!
     *
     * @param max The max to use
     * @return A new DetachedCriteria instance derived from this
     */
    DetachedCriteria<T> max(int max) {
        DetachedCriteria newCriteria = this.clone()
        newCriteria.defaultMax = max
        return newCriteria
    }

    /**
     * Sets the default offset to use and returns a new criteria instance. This method does not mutate the original criteria!
     *
     * @param offset The offset to use
     * @return A new DetachedCriteria instance derived from this
     */
    DetachedCriteria<T> offset(int offset) {
        DetachedCriteria newCriteria = this.clone()
        newCriteria.defaultOffset = offset
        return newCriteria
    }

    /**
     * Adds a sort order to this criteria instance
     *
     * @param property The property to sort by
     * @return This criteria instance
     */
    DetachedCriteria<T> sort(String property) {
        DetachedCriteria newCriteria = this.clone()
        newCriteria.orders.add(new Order(property))
        return newCriteria
    }

    /**
     * Adds a sort order to this criteria instance
     *
     * @param property The property to sort by
     * @param direction The direction to sort by
     * @return This criteria instance
     */
    DetachedCriteria<T> sort(String property, String direction) {
        DetachedCriteria newCriteria = this.clone()
        newCriteria.orders.add(new Order(property, "desc".equalsIgnoreCase(direction) ? Direction.DESC : Direction.ASC))
        return newCriteria
    }

    /**
     * Adds a property projection
     *
     * @param property The property to sort by
     * @return This criteria instance
     */
    DetachedCriteria<T> property(String property) {
        DetachedCriteria newCriteria = this.clone()
        newCriteria.projectionList.property(property)
        return newCriteria
    }

    /**
     * Adds a distinct property projection
     *
     * @param property The property to obtain the distinct value for
     * @return This criteria instance
     */
    DetachedCriteria<T> distinct(String property) {
        DetachedCriteria newCriteria = this.clone()
        newCriteria.projectionList.distinct(property)
        return newCriteria
    }

    /**
     * Method missing handler that deals with the invocation of dynamic finders
     *
     * @param methodName The method name
     * @param args The arguments
     * @return The result of the method call
     */
    def methodMissing(String methodName, args) {
        initialiseIfNecessary(targetClass)
        def method = dynamicFinders.find { FinderMethod f -> f.isMethodMatch(methodName) }
        if(method != null) {
            return method.invoke(targetClass, methodName,this, args)
        }
        else if(args && args.size() == 1 && (args[-1] instanceof Closure)) {
            final prop = persistentEntity.getPropertyByName(methodName)
            if(prop instanceof Association) {
                def associationCriteria = new DetachedAssociationCriteria(prop.associatedEntity.javaClass, prop)
                add associationCriteria
                final callable = args[-1]
                callable.delegate = associationCriteria
                callable.call()
            }
            else {
                throw new MissingMethodException(methodName, DetachedCriteria, args)
            }
        }
        else {
            throw new MissingMethodException(methodName, DetachedCriteria, args)
        }
    }

    @Override
    Iterator<T> iterator() {
        return list().iterator()
    }

    @Override
    protected DetachedCriteria<T> clone() {
        def criteria = new DetachedCriteria(targetClass)
        criteria.criteria = new ArrayList(this.criteria)
        final projections = new ArrayList(this.projections)
        criteria.projections = projections
        criteria.projectionList = new DetachedProjections(projections)
        criteria.orders = new ArrayList(this.orders)
        criteria.defaultMax = defaultMax
        criteria.defaultOffset = defaultOffset
        return criteria
    }

    protected void handleJunction(Closure callable) {
        try {
            callable.delegate = this
            callable.call()

        }
        finally {
            def lastJunction = junctions.remove(junctions.size() - 1)
            add lastJunction
        }
    }

    private withPopulatedQuery(Map args, Closure additionalCriteria, Closure callable)  {
        targetClass.withDatastoreSession { Session session ->
            Query query = session.createQuery(targetClass)
            if(defaultMax != null) {
                query.max(defaultMax)
            }
            if(defaultOffset != null) {
                query.offset(defaultOffset)
            }
            DynamicFinder.applyDetachedCriteria(query, this)

            if(additionalCriteria != null) {
                def additionalDetached = new DetachedCriteria(targetClass).build( additionalCriteria )
                DynamicFinder.applyDetachedCriteria(query, additionalDetached)
            }

            DynamicFinder.populateArgumentsForCriteria(targetClass, query, args)

            callable.call(query)
        }
    }

}
