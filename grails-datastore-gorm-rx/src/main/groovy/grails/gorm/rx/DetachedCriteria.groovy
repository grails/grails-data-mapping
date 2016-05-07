package grails.gorm.rx

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import org.grails.datastore.gorm.finders.DynamicFinder
import org.grails.datastore.gorm.query.criteria.AbstractDetachedCriteria
import org.grails.datastore.mapping.query.Query
import org.grails.datastore.mapping.query.api.QueryArgumentsAware
import org.grails.datastore.mapping.query.api.QueryableCriteria
import org.grails.datastore.rx.query.RxQuery
import org.grails.gorm.rx.api.RxGormEnhancer
import rx.Observable

/**
 * Reactive version of {@link grails.gorm.DetachedCriteria}
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@InheritConstructors
@CompileStatic
class DetachedCriteria<T> extends AbstractDetachedCriteria<Observable<T>> {
    /**
     * Finds a single result matching this criteria. Note that the observable returned will emit each result one by one. If you
     * prefer to receive the entire list of results use {@link #toList()} instead
     *
     * @param args The arguments The arguments
     * @param additionalCriteria Any additional criteria
     *
     * @return An observable
     */
    Observable<T> find(Map args = Collections.emptyMap(), @DelegatesTo(DetachedCriteria) Closure additionalCriteria = null) {
        Query query = prepareQuery(args, additionalCriteria)
        query.max(1)
        return ((RxQuery)query).findAll()
    }

    /**
     * Finds all results matching this criteria. Note that the observable returned will emit each result one by one. If you
     * prefer to receive the entire list of results use {@link #toList()} instead
     *
     * @param args The arguments The arguments
     * @param additionalCriteria Any additional criteria
     *
     * @return An observable
     */
    Observable<T> findAll(Map args = Collections.emptyMap(), @DelegatesTo(DetachedCriteria) Closure additionalCriteria = null) {
        Query query = prepareQuery(args, additionalCriteria)
        return ((RxQuery)query).findAll()
    }

    /**
     * The same as {@link #find(java.util.Map, groovy.lang.Closure)}
     *
     * @see #find(java.util.Map, groovy.lang.Closure)
     */
    Observable<T> get(Map args, @DelegatesTo(DetachedCriteria) Closure additionalCriteria) {
        Query query = prepareQuery(args, additionalCriteria)
        query.max(1)
        return ((RxQuery)query).singleResult()
    }

    /**
     * Converts the observable to another observable that outputs the complete list. Not for use with large datasets
     *
     * @param args The arguments
     * @param additionalCriteria Any additional criteria
     * @return An observable that emits a list
     */
    Observable<List<T>> toList(Map args = Collections.emptyMap(), @DelegatesTo(DetachedCriteria) Closure additionalCriteria = null) {
        Query query = prepareQuery(args, additionalCriteria)
        return ((RxQuery)query).findAll().toList()
    }

    /**
     * Converts the observable to another observable that outputs the complete list. Not for use with large datasets
     *
     * @param args The arguments
     * @param additionalCriteria Any additional criteria
     * @return An observable that emits a list
     */
    Observable<List<T>> list(Map args = Collections.emptyMap(), @DelegatesTo(DetachedCriteria) Closure additionalCriteria = null) {
        Query query = prepareQuery(args, additionalCriteria)
        return ((RxQuery)query).findAll().toList()
    }

    /**
     * Calculates the total number of matches for the query
     *
     * @param args The arguments
     * @param additionalCriteria Any additional criteria
     * @return The total results
     */
    Observable<Number> getCount(Map args = Collections.emptyMap(), @DelegatesTo(DetachedCriteria) Closure additionalCriteria = null) {
        Query query = prepareQuery(args, additionalCriteria)
        query.projections().count()
        return ((RxQuery)query).singleResult()
    }

    /**
     * The same as {@link #getCount(java.util.Map, groovy.lang.Closure)}
     *
     * @see #getCount(java.util.Map, groovy.lang.Closure)
     */
    Observable<Number> count(Map args = Collections.emptyMap(), @DelegatesTo(DetachedCriteria) Closure additionalCriteria = null) {
        getCount(args, additionalCriteria)
    }

    /**
     * Updates all entities matching this criteria
     *
     * @param propertiesMap The property names and values to update
     *
     * @return An observable that returns the total number updated
     */
    Observable<Number> updateAll(Map propertiesMap) {
        Query query = prepareQuery(Collections.emptyMap(), null)
        return ((RxQuery)query).updateAll(propertiesMap)
    }

    /**
     * Deletes all entities matching this criteria
     *
     * @return An observable that returns the total number deleted
     */
    Observable<Number> deleteAll() {
        Query query = prepareQuery(Collections.emptyMap(), null)
        return ((RxQuery)query).deleteAll()
    }

    @Override
    DetachedCriteria<Observable<T>> where(@DelegatesTo(AbstractDetachedCriteria) Closure additionalQuery) {
        return (DetachedCriteria) super.where(additionalQuery)
    }

    @Override
    DetachedCriteria<Observable<T>> whereLazy(@DelegatesTo(AbstractDetachedCriteria) Closure additionalQuery) {
        return (DetachedCriteria) super.whereLazy(additionalQuery)
    }

    @Override
    DetachedCriteria<Observable<T>> build(@DelegatesTo(grails.gorm.DetachedCriteria) Closure callable) {
        return (DetachedCriteria)super.build(callable)
    }

    @Override
    DetachedCriteria<Observable<T>> buildLazy(@DelegatesTo(grails.gorm.DetachedCriteria) Closure callable) {
        return (DetachedCriteria)super.buildLazy(callable)
    }

    @Override
    DetachedCriteria<Observable<T>> max(int max) {
        return (DetachedCriteria)super.max(max)
    }

    @Override
    DetachedCriteria<Observable<T>> offset(int offset) {
        return (DetachedCriteria)super.offset(offset)
    }

    @Override
    DetachedCriteria<Observable<T>> sort(String property) {
        return (DetachedCriteria)super.sort(property)
    }

    @Override
    DetachedCriteria<Observable<T>> sort(String property, String direction) {
        return (DetachedCriteria)super.sort(property, direction)
    }

    @Override
    DetachedCriteria<Observable<T>> property(String property) {
        return (DetachedCriteria)super.property(property)
    }

    @Override
    DetachedCriteria<Observable<T>> id() {
        return (DetachedCriteria)super.id()
    }

    @Override
    DetachedCriteria<Observable<T>> avg(String property) {
        return (DetachedCriteria)super.avg(property)
    }

    @Override
    DetachedCriteria<Observable<T>> sum(String property) {
        return (DetachedCriteria)super.sum(property)
    }

    @Override
    DetachedCriteria<Observable<T>> min(String property) {
        return (DetachedCriteria)super.min(property)
    }

    @Override
    DetachedCriteria<Observable<T>> max(String property) {
        return (DetachedCriteria)super.max(property)
    }

    @Override
    def propertyMissing(String name) {
        return super.propertyMissing(name)
    }

    @Override
    DetachedCriteria<Observable<T>> distinct(String property) {
        return (DetachedCriteria)super.distinct(property)
    }

    @Override
    protected QueryableCriteria buildQueryableCriteria(Closure queryClosure) {
        return (QueryableCriteria)new DetachedCriteria(targetClass).build(queryClosure)
    }

    @Override
    protected DetachedCriteria<T> clone() {
        return (DetachedCriteria)super.clone()
    }

    @Override
    protected DetachedCriteria newInstance() {
        new DetachedCriteria(targetClass, alias)
    }


    protected Query prepareQuery(Map args, Closure additionalCriteria) {
        def staticApi = RxGormEnhancer.findStaticApi(targetClass)
        applyLazyCriteria()
        def query = staticApi.datastoreClient.createQuery(targetClass)
        if (defaultMax != null) {
            query.max(defaultMax)
        }
        if (defaultOffset != null) {
            query.offset(defaultOffset)
        }
        DynamicFinder.applyDetachedCriteria(query, this)

        if (query instanceof QueryArgumentsAware) {
            query.arguments = args
        }

        if (additionalCriteria != null) {
            def additionalDetached = new DetachedCriteria(targetClass).build(additionalCriteria)
            DynamicFinder.applyDetachedCriteria(query, additionalDetached)
        }

        DynamicFinder.populateArgumentsForCriteria(targetClass, query, args)
        query
    }

}
