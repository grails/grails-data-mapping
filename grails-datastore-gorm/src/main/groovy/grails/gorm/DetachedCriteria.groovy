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

/**
 * Represents criteria that is not bound to the current connection and can be built up and re-used at a later date
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class DetachedCriteria<T> implements Criteria, Cloneable {
    
    private List<Criterion> criteria = []
    private List<Order> orders = []
    private List<Projection> projections = []
    private Class targetClass

    ProjectionList projectionList = new DetachedProjections(projections)

    /**
     * Constructs a DetachedCriteria instance target the given class
     * @param targetClass
     */
    DetachedCriteria(Class<T> targetClass) {
        this.targetClass = targetClass
    }

    public void add(Criterion criterion) {
        criteria << criterion
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
        return this
    }

    /**
     * @see Criteria
     */
    Criteria order(String propertyName, String direction) {
        return this
    }

    /**
     * @see Criteria
     */
    Criteria inList(String propertyName, Collection values) {
        criteria << Restrictions.in(propertyName, values)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria inList(String propertyName, Object[] values) {
        criteria << Restrictions.in(propertyName, Arrays.asList(values))
        return this
    }

    /**
     * @see Criteria
     */
    Criteria sizeEq(String propertyName, int size) {
        criteria << Restrictions.sizeEq(propertyName, size)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria sizeGt(String propertyName, int size) {
        criteria << Restrictions.sizeGt(propertyName, size)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria sizeGe(String propertyName, int size) {
        criteria << Restrictions.sizeGe(propertyName, size)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria sizeLe(String propertyName, int size) {
        criteria << Restrictions.sizeLe(propertyName, size)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria sizeLt(String propertyName, int size) {
        criteria << Restrictions.sizeLt(propertyName, size)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria sizeNe(String propertyName, int size) {
        criteria << Restrictions.sizeNe(propertyName, size)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria eqProperty(String propertyName, String otherPropertyName) {
        criteria << Restrictions.eqProperty(propertyName,otherPropertyName)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria neProperty(String propertyName, String otherPropertyName) {
        criteria << Restrictions.neProperty(propertyName,otherPropertyName)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria gtProperty(String propertyName, String otherPropertyName) {
        criteria << Restrictions.gtProperty(propertyName,otherPropertyName)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria geProperty(String propertyName, String otherPropertyName) {
        criteria << Restrictions.geProperty(propertyName,otherPropertyName)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria ltProperty(String propertyName, String otherPropertyName) {
        criteria << Restrictions.ltProperty(propertyName,otherPropertyName)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria leProperty(String propertyName, String otherPropertyName) {
        criteria << Restrictions.leProperty(propertyName,otherPropertyName)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria idEquals(Object value) {
        criteria << Restrictions.idEq(value)
        return this
    }


    /**
     * @see Criteria
     */
    Criteria isEmpty(String propertyName) {
        criteria << Restrictions.isEmpty(propertyName)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria isNotEmpty(String propertyName) {
        criteria << Restrictions.isNotEmpty(propertyName)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria isNull(String propertyName) {
        criteria << Restrictions.isNull(propertyName)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria isNotNull(String propertyName) {
        criteria << Restrictions.isNotNull(propertyName)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria eq(String propertyName, Object propertyValue) {
        criteria << Restrictions.eq(propertyName,propertyValue)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria idEq(Object propertyValue) {
        criteria << Restrictions.idEq(propertyValue)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria ne(String propertyName, Object propertyValue) {
        criteria << Restrictions.ne(propertyName,propertyValue)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria between(String propertyName, Object start, Object finish) {
        criteria << Restrictions.between(propertyName, start, finish)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria gte(String property, Object value) {
        criteria << Restrictions.gte(property,value)
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
        criteria << Restrictions.gt(property,value)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria lte(String property, Object value) {
        criteria << Restrictions.lte(property, value)
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
        criteria << Restrictions.lt(property,value)
        return this
    }

    /**
     * @see Criteria
     */
    Criteria like(String propertyName, Object propertyValue) {
        criteria << Restrictions.like(propertyName,propertyValue.toString())
        return this
    }

    /**
     * @see Criteria
     */
    Criteria ilike(String propertyName, Object propertyValue) {
        criteria << Restrictions.ilike(propertyName, propertyValue.toString())
        return this
    }

    /**
     * @see Criteria
     */
    Criteria rlike(String propertyName, Object propertyValue) {
        criteria << Restrictions.rlike(propertyName, propertyValue.toString())
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
     * Lists all records matching the criterion contained within this DetachedCriteria instance
     *
     * @return A list of matching instances
     */
    List<T> list( Map args = Collections.emptyMap(), Closure additionalCriteria = null) {
        (List)withPopulatedQuery(args, additionalCriteria) { Query query ->
            query.list()
        }
    }

    private withPopulatedQuery(Map args, Closure additionalCriteria, Closure callable)  {
        targetClass.withDatastoreSession { Session session ->
            Query query = session.createQuery(targetClass)
            populateQueryFromDetached(this, query)

            if(additionalCriteria != null) {
                def additionalDetached = new DetachedCriteria(targetClass)
                additionalDetached.build additionalCriteria
                populateQueryFromDetached(additionalDetached, query)
            }

            DynamicFinder.populateArgumentsForCriteria(targetClass, query, args)

            callable.call(query)
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
     * Enable the builder syntax for contructing Criteria
     *
     * @param callable The callable closure
     * @return This criteria instance
     */

    Criteria build(Closure callable) {
        this.with callable
        return this
    }
    /**
     * Counts all the records matching the criterion contained within this DetachedCriteria instance
     * @param args Any arguments to the query
     * @return The count of all records
     */

    protected void populateQueryFromDetached(DetachedCriteria current, Query query) {
        for (criterion in current.criteria) {
            query.add criterion
        }
        final projectionList = query.projections()
        for (projection in current.projections) {
            projectionList.add projection
        }
        for (order in current.orders) {
            query.order(order)
        }
    }

    @Override
    protected DetachedCriteria<T> clone() {
        def criteria = new DetachedCriteria(targetClass)
        criteria.criteria = this.criteria
        criteria.projections = this.projections
        criteria.orders = this.orders
        return this
    }


}
