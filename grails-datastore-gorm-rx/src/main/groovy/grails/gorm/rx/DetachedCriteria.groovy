package grails.gorm.rx

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import org.grails.datastore.gorm.finders.DynamicFinder
import org.grails.datastore.mapping.query.Query
import org.grails.datastore.mapping.query.api.QueryArgumentsAware
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
class DetachedCriteria<T> extends grails.gorm.DetachedCriteria<Observable<T>> {
    /**
     * Finds a single result matching this criteria. Note that the observable returned will emit each result one by one. If you
     * prefer to receive the entire list of results use {@link #toList()} instead
     *
     * @param args The arguments The arguments
     * @param additionalCriteria Any additional criteria
     *
     * @return An observable
     */
    @Override
    Observable<T> find(Map args, @DelegatesTo(grails.gorm.DetachedCriteria) Closure additionalCriteria) {
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
    Observable<T> findAll(Map args = Collections.emptyMap(), @DelegatesTo(grails.gorm.DetachedCriteria) Closure additionalCriteria = null) {
        Query query = prepareQuery(args, additionalCriteria)
        return ((RxQuery)query).findAll()
    }

    /**
     * The same as {@link #find(java.util.Map, groovy.lang.Closure)}
     *
     * @see #find(java.util.Map, groovy.lang.Closure)
     */
    @Override
    Observable<T> get(Map args, @DelegatesTo(grails.gorm.DetachedCriteria) Closure additionalCriteria) {
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
    Observable<List<T>> toList(Map args = Collections.emptyMap(), @DelegatesTo(grails.gorm.DetachedCriteria) Closure additionalCriteria = null) {
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
    Observable<Number> total(Map args = Collections.emptyMap(), @DelegatesTo(grails.gorm.DetachedCriteria) Closure additionalCriteria = null) {
        Query query = prepareQuery(args, additionalCriteria)
        query.projections().count()
        return ((RxQuery)query).singleResult()
    }

    @Override
    List<Observable<T>> list(Map args, @DelegatesTo(grails.gorm.DetachedCriteria) Closure additionalCriteria) {
        throw new UnsupportedOperationException("Method list() is blocking. Use findAll() or toList() instead")
    }

    @Override
    Number count(Map args, @DelegatesTo(grails.gorm.DetachedCriteria) Closure additionalCriteria) {
        throw new UnsupportedOperationException("Method count() is blocking. Use total() instead")
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
    protected DetachedCriteria<Observable<T>> clone() {
        return (DetachedCriteria<Observable<T>>)super.clone()
    }

    @Override
    protected grails.gorm.DetachedCriteria newInstance() {
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
