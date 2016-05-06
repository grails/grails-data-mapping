package grails.gorm.rx

import groovy.transform.InheritConstructors
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.query.Query
import org.grails.datastore.mapping.query.QueryCreator
import org.grails.datastore.mapping.query.api.Criteria
import org.grails.datastore.rx.query.RxQuery
import rx.Observable

import static org.grails.datastore.gorm.finders.DynamicFinder.populateArgumentsForCriteria

/**
 * A CriteriaBuilder implementation for RxGORM
 *
 * @see grails.gorm.CriteriaBuilder
 *
 * @author Graeme Rocher
 * @since 6.0
 */
class CriteriaBuilder<T> extends grails.gorm.CriteriaBuilder {

    CriteriaBuilder(Class<T> targetClass, QueryCreator queryCreator, MappingContext mappingContext) {
        super(targetClass, queryCreator, mappingContext)
    }

    /**
     * Finds a single result matching this criteria. Note that the observable returned will emit each result one by one. If you
     * prefer to receive the entire list of results use {@link #toList()} instead
     *
     * @param callable Any additional criteria
     *
     * @return An observable
     */
    @Override
    Observable<T> get(Closure callable) {
        return (Observable<T>)super.get(callable)
    }

    /**
     * Finds a single result matching this criteria. Note that the observable returned will emit each result one by one. If you
     * prefer to receive the entire list of results use {@link #toList()} instead
     *
     * @param args The arguments The arguments
     * @param additionalCriteria Any additional criteria
     *
     * @return An observable
     */
    Observable<T> get() {
        ensureQueryIsInitialized()
        uniqueResult = true
        return (Observable<T>)query.singleResult()
    }

    /**
     * Finds a single result matching this criteria. Note that the observable returned will emit each result one by one. If you
     * prefer to receive the entire list of results use {@link #toList()} instead
     *
     * @param callable Any additional criteria
     *
     * @return An observable
     */
    Observable<T> find(Closure callable = null) {
        return get(callable)
    }

    /**
     * Finds a multiple results matching this criteria. Note that the observable returned will emit each result one by one. If you
     * prefer to receive the entire list of results use {@link #toList()} instead
     *
     * @param additionalCriteria Any additional criteria
     *
     * @return An observable
     */
    Observable<T> findAll(@DelegatesTo(grails.gorm.DetachedCriteria) Closure additionalCriteria) {
        findAll (Collections.emptyMap(), additionalCriteria)
    }

    /**
     * Finds all results for this criteria
     *
     * @param args The arguments
     * @param additionalCriteria Any additional criteria
     * @return An observable that emits a list
     */
    Observable<T> findAll(Map args = Collections.emptyMap(), @DelegatesTo(grails.gorm.DetachedCriteria) Closure additionalCriteria = null) {
        prepareQuery(args, additionalCriteria)

        return ((RxQuery) query).findAll()
    }

    /**
     * Converts the observable to another observable that outputs the complete list. Not for use with large datasets
     *
     * @param args The arguments
     * @param additionalCriteria Any additional criteria
     * @return An observable that emits a list
     */
    Observable<List<T>> toList(Map args = Collections.emptyMap(), @DelegatesTo(grails.gorm.DetachedCriteria) Closure additionalCriteria = null) {
        findAll(args, additionalCriteria).toList()
    }

    /**
     * Calculates the total number of matches for the query
     *
     * @param args The arguments
     * @param additionalCriteria Any additional criteria
     * @return The total results
     */
    Observable<Number> total(Map args = Collections.emptyMap(), @DelegatesTo(grails.gorm.DetachedCriteria) Closure additionalCriteria = null) {
        Query query = prepareQuery(args, additionalCriteria)
        query.projections().count()
        return ((RxQuery)query).singleResult()
    }

    @Override
    List list(Closure callable) {
        throw new UnsupportedOperationException("Method list() is blocking. Use findAll() or toList() instead")
    }

    @Override
    List listDistinct(Closure callable) {
        throw new UnsupportedOperationException("Method listDistinct() is blocking. Use findAll() or toList() instead")
    }

    @Override
    List list(Map paginateParams, Closure callable) {
        throw new UnsupportedOperationException("Method list() is blocking. Use findAll() or toList() instead")
    }

    @Override
    Number count(Closure callable) {
        throw new UnsupportedOperationException("Method count() is blocking. Use total() instead")
    }

    @Override
    Object scroll(@DelegatesTo(Criteria.class) Closure c) {
        throw new UnsupportedOperationException("Method scroll() not implemented")
    }

    protected void prepareQuery(Map args, Closure additionalCriteria) {
        ensureQueryIsInitialized()
        populateArgumentsForCriteria(targetClass, query, args);
        invokeClosureNode(additionalCriteria)
        for (Query.Order orderEntry : orderEntries) {
            query.order(orderEntry);
        }
    }

    @Override
    protected Object invokeList() {
        return ((RxQuery)query).findAll()
    }
}
