package org.grails.gorm.rx.api

import grails.gorm.rx.CriteriaBuilder
import grails.gorm.rx.DetachedCriteria
import grails.gorm.rx.RxEntity
import grails.gorm.rx.api.RxGormStaticOperations
import grails.gorm.rx.proxy.ObservableProxy
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.codehaus.groovy.runtime.InvokerHelper
import org.grails.datastore.gorm.GormValidateable
import org.grails.datastore.gorm.finders.DynamicFinder
import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.query.Query
import org.grails.datastore.mapping.query.api.Criteria
import org.grails.datastore.mapping.validation.ValidationException
import org.grails.datastore.rx.RxDatastoreClient
import org.grails.datastore.rx.query.RxQuery
import org.grails.gorm.rx.finders.*
import org.springframework.beans.PropertyAccessorFactory
import rx.Observable
import rx.Subscriber
/**
 * Bridge to the implementation of the static method level operations for RX GORM
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class RxGormStaticApi<D> implements RxGormStaticOperations<D> {

    final PersistentEntity entity
    final RxDatastoreClient datastoreClient
    final Class persistentClass

    final List<FinderMethod> gormDynamicFinders

    RxGormStaticApi(PersistentEntity entity, RxDatastoreClient datastoreClient) {
        this.entity = entity
        this.persistentClass = entity.getJavaClass()
        this.datastoreClient = datastoreClient
        this.gormDynamicFinders = createDynamicFinders()
    }

    /**
     * Retrieve an instance by id
     *
     * @param id The id of the instance
     * @return An observable
     */
    @Override
    D create() {
        (D)entity.newInstance()
    }

    @Override
    Observable<D> get(Serializable id, Map args = Collections.emptyMap()) {
        def clazz = entity.javaClass
        def query = datastoreClient.createQuery(clazz)
        query.idEq(id)
        query.max(1)
        DynamicFinder.populateArgumentsForCriteria(clazz, query, args)
        return ((RxQuery<D>)query).singleResult(args)
    }

    /**
     * Finds the first object sorted by propertyName
     *
     * @param propertyName the name of the property to sort by
     *
     * @return A single that will emit the first object, if it exists
     */
    Observable<D> first(String property) {
        first(sort:property)
    }

    /**
     * Finds the first object.  If queryParams includes 'sort', that will
     * dictate the sort order, otherwise natural sort order will be used.
     * queryParams may include any of the same parameters that might be passed
     * to the list(Map) method.  This method will ignore 'order' and 'max' as
     * those are always 'asc' and 1, respectively.
     *
     * @return A single that will emit the first object, if it exists
     */
    Observable<D> first(Map params = Collections.emptyMap()) {
        def q = datastoreClient.createQuery(persistentClass)
        Map<String,Object> newParams = new LinkedHashMap<>(params)
        newParams.remove('order')
        DynamicFinder.populateArgumentsForCriteria(persistentClass, q, newParams)
        q.max(1)
        ((RxQuery)q).singleResult(newParams)
    }


    /**
     * Finds the last object sorted by propertyName
     *
     * @param propertyName the name of the property to sort by
     *
     * @return A single that will emit the first object, if it exists
     */
    Observable<D> last(String property) {
        last(sort:property)
    }

    /**
     * Finds the last object.  If queryParams includes 'sort', that will
     * dictate the sort order, otherwise natural sort order will be used.
     * queryParams may include any of the same parameters that might be passed
     * to the list(Map) method.  This method will ignore 'order' and 'max' as
     * those are always 'desc' and 1, respectively.
     *
     * @return A single that will emit the last object, if it exists
     */
    Observable<D> last(Map params = Collections.emptyMap()) {
        def q = datastoreClient.createQuery(persistentClass)
        Map<String,Object> newParams = new LinkedHashMap<>(params)
        newParams.put('order', 'desc')
        if(!newParams.containsKey('sort')) {
            newParams.put('sort', entity.identity.name)
        }
        DynamicFinder.populateArgumentsForCriteria(persistentClass, q, newParams)
        q.max(1)
        ((RxQuery)q).singleResult(newParams)
    }

    /**
     * @return Counts the number of instances
     */
    Observable<Number> count() {
        def query = datastoreClient.createQuery(entity.javaClass)
        query.projections().count()
        return ((RxQuery)query).singleResult()
    }

    @Override
    Observable<Number> deleteAll(D... objects) {
        return deleteAll(Arrays.asList(objects))
    }

    /**
     * Batch deletes a number of objects in one go
     *
     * @param objects The objects to delete
     * @return The number of objects actually deleted
     */
    Observable<Number> deleteAll(Iterable objects) {
        datastoreClient.deleteAll(objects)
    }

    /**
     * Batch saves all of the given objects
     *
     * @param objects The objects to save
     * @return An observable that emits the identifiers of the saved objects
     */
    @Override
    Observable<List<Serializable>> saveAll(Iterable<D> objects, Map arguments = Collections.emptyMap()) {
        boolean shouldValidate = arguments?.containsKey("validate") ? arguments.validate : true
        if(shouldValidate) {
            def firstInvalid = objects.find() {
                (it instanceof GormValidateable) && !((GormValidateable)it).validate()
            }
            if(firstInvalid != null) {
                throw new ValidationException("Validation error occurred during call to save() for entity [$firstInvalid]", ((GormValidateable)firstInvalid).errors)
            }
            else {
                return datastoreClient.persistAll(objects)
            }
        }
        else {
            return datastoreClient.persistAll(objects)
        }
    }

    @Override
    Observable<List<Serializable>> saveAll(D... objects) {
        saveAll(Arrays.asList(objects))
    }

    @Override
    Observable<Boolean> exists(Serializable id) {
        get(id).map { it != null }.defaultIfEmpty(false)
    }

    Observable<List<D>> list(Map params = Collections.emptyMap()) {
        def query = datastoreClient.createQuery(entity.javaClass)
        DynamicFinder.populateArgumentsForCriteria(entity.javaClass, query, params)
        return ((RxQuery<D>) query).findAll(params).toList()
    }

    Observable<D> findAll(Map params = Collections.emptyMap()) {
        def query = datastoreClient.createQuery(entity.javaClass)
        DynamicFinder.populateArgumentsForCriteria(entity.javaClass, query, params)
        return ((RxQuery<D>) query).findAll(params)
    }

    /**
     * Finds a single result matching all of the given conditions. Eg. Book.findWhere(author:"Stephen King", title:"The Stand")
     *
     * @param queryMap The map of conditions
     * @return A single result
     */
    Observable<D> findWhere(Map queryMap) {
        findWhere(queryMap, Collections.emptyMap())
    }

    /**
     * Finds a single result matching all of the given conditions. Eg. Book.findWhere(author:"Stephen King", title:"The Stand")
     *
     * @param queryMap The map of conditions
     * @param args The Query arguments
     *
     * @return A single result
     */
    Observable<D> findWhere(Map queryMap, Map args) {
        def query = datastoreClient.createQuery(entity.javaClass)
        DynamicFinder.populateArgumentsForCriteria(entity.javaClass, query, args)
        query.allEq(queryMap)
        query.max(1)
        ((RxQuery<D>)query).singleResult(args)
    }


    /**
     * Finds a single result matching all of the given conditions. Eg. Book.findWhere(author:"Stephen King", title:"The Stand").  If
     * a matching persistent entity is not found a new entity is created and returned.
     *
     * @param queryMap The map of conditions
     * @return A single result
     */
    Observable<D> findOrCreateWhere(Map queryMap) {
        findWhere(queryMap)
            .switchIfEmpty(Observable.create({ Subscriber s ->
            s.onNext(entity.javaClass.newInstance(queryMap))
            s.onCompleted()
        } as Observable.OnSubscribe))
    }


    /**
     * Finds a single result matching all of the given conditions. Eg. Book.findWhere(author:"Stephen King", title:"The Stand").  If
     * a matching persistent entity is not found a new entity is created, saved and returned.
     *
     * @param queryMap The map of conditions
     * @return A single result
     */

    Observable<D> findOrSaveWhere(Map queryMap) {
        findWhere(queryMap)
                .switchIfEmpty(Observable.create({ Subscriber s ->
            Thread.start {
                def instance = entity.javaClass.newInstance(queryMap)
                ((RxEntity)instance).save().subscribe(new Subscriber() {
                    @Override
                    void onCompleted() {
                        s.onCompleted()
                    }

                    @Override
                    void onError(Throwable e) {
                        s.onError(e)
                    }

                    @Override
                    void onNext(Object o) {
                        s.onNext(o)
                    }
                })
            }
        } as Observable.OnSubscribe ))
    }

    /**
     * Finds a single result matching all of the given conditions. Eg. Book.findWhere(author:"Stephen King", title:"The Stand")
     *
     * @param queryMap The map of conditions
     * @return A single result
     */
    Observable<D> findAllWhere(Map queryMap) {
        findAllWhere(queryMap, Collections.emptyMap())
    }

    /**
     * Finds a single result matching all of the given conditions. Eg. Book.findWhere(author:"Stephen King", title:"The Stand")
     *
     * @param queryMap The map of conditions
     * @param args The Query arguments
     *
     * @return A single result
     */
    Observable<D> findAllWhere(Map queryMap, Map args) {
        def query = datastoreClient.createQuery(entity.javaClass)
        DynamicFinder.populateArgumentsForCriteria(entity.javaClass, query, args)
        query.allEq(queryMap)
        ((RxQuery<D>)query).findAll(args)
    }

    /**
     *
     * @param callable Callable closure containing detached criteria definition
     * @return The DetachedCriteria instance
     */
    DetachedCriteria<D> where(Closure callable) {
        new DetachedCriteria<D>(persistentClass).build(callable)
    }

    /**
     * Uses detached criteria to build a query and then execute it returning an observable
     *
     * @param callable The callable
     * @return The observable
     */
    Observable<D> findAll(Closure callable) {
        new DetachedCriteria<D>(persistentClass)
                .build(callable)
                .findAll()
    }

    /**
     * Uses detached criteria to build a query and then execute it returning an observable
     *
     * @param callable The callable
     * @return The observable
     */
    Observable<D> find(Closure callable) {
        new DetachedCriteria<D>(persistentClass)
                .build(callable)
                .find()
    }

    /**
     *
     * @param callable Callable closure containing detached criteria definition
     * @return The DetachedCriteria instance that is lazily initialized
     */
    DetachedCriteria<D> whereLazy(Closure callable) {
        new DetachedCriteria<D>(persistentClass).buildLazy(callable)
    }
    /**
     *
     * @param callable Callable closure containing detached criteria definition
     * @return The DetachedCriteria instance
     */
    DetachedCriteria<D> whereAny(Closure callable) {
        (DetachedCriteria<D>)new DetachedCriteria<D>(persistentClass).or(callable)
    }


    /**
     * Creates a criteria builder instance
     */
    CriteriaBuilder<D> createCriteria() {
        new CriteriaBuilder(persistentClass, datastoreClient, datastoreClient.mappingContext)
    }

    /**
     * Creates a criteria builder instance
     */

    Observable withCriteria(@DelegatesTo(Criteria) Closure callable) {
        createCriteria().findAll(callable)
    }

    /**
     * Creates a criteria builder instance
     */
    Observable withCriteria(Map builderArgs, @DelegatesTo(Criteria) Closure callable) {
        def criteriaBuilder = createCriteria()
        def builderBean = PropertyAccessorFactory.forBeanPropertyAccess(criteriaBuilder)
        for (entry in builderArgs.entrySet()) {
            String propertyName = entry.key.toString()
            if (builderBean.isWritableProperty(propertyName)) {
                builderBean.setPropertyValue(propertyName, entry.value)
            }
        }

        if(builderArgs?.uniqueResult) {
            return criteriaBuilder.get(callable)

        }
        else {
            return criteriaBuilder.findAll(callable)
        }

    }

    @Override
    Observable<D> staticMethodMissing(String methodName, Object arg) {
        return methodMissing(methodName, arg)
    }

    @Override
    Object staticPropertyMissing(String property) {
        return propertyMissing(property)
    }

    @CompileDynamic
    Observable<D> methodMissing(String methodName, args) {
        FinderMethod method = gormDynamicFinders.find { FinderMethod f -> f.isMethodMatch(methodName) }
        if (!method) {
            throw new MissingMethodException(methodName, persistentClass, args)
        }

        def mc = persistentClass.getMetaClass()

        // register the method invocation for next time
        mc.static."$methodName" = { Object[] varArgs ->
            // FYI... This is relevant to http://jira.grails.org/browse/GRAILS-3463 and may
            // become problematic if http://jira.codehaus.org/browse/GROOVY-5876 is addressed...
            final argumentsForMethod
            if(varArgs == null) {
                argumentsForMethod = [null] as Object[]
            }
            // if the argument component type is not an Object then we have an array passed that is the actual argument
            else if(varArgs.getClass().componentType != Object) {
                // so we wrap it in an object array
                argumentsForMethod = [varArgs] as Object[]
            }
            else {

                if(varArgs.length == 1 && varArgs[0].getClass().isArray()) {
                    argumentsForMethod = varArgs[0]
                } else {

                    argumentsForMethod = varArgs
                }
            }
            method.invoke(delegate, methodName, argumentsForMethod)
        }

        return method.invoke(persistentClass, methodName, args)
    }


    /**
     * Property missing handler
     *
     * @param name The name of the property
     */
    def propertyMissing(String name) {
        throw new MissingPropertyException(name, persistentClass)
    }

    protected List<FinderMethod> createDynamicFinders() {
        [new FindOrCreateByFinder(datastoreClient),
         new FindOrSaveByFinder(datastoreClient),
         new FindByFinder(datastoreClient),
         new FindAllByFinder(datastoreClient),
         new CountByFinder(datastoreClient),
         new FindByBooleanFinder(datastoreClient),
         new FindAllByBooleanFinder(datastoreClient)] as List<FinderMethod>
    }

    @Override
    ObservableProxy<D> proxy(Serializable id) {
        datastoreClient.proxy(entity.javaClass, id)
    }

    @Override
    ObservableProxy<D> proxy(Serializable id, Map queryArgs) {
        datastoreClient.proxy(entity.javaClass, id)
    }

    @Override
    ObservableProxy<D> proxy(DetachedCriteria<D> query) {
        datastoreClient.proxy(query.toQuery())
    }

    @Override
    ObservableProxy<D> proxy(DetachedCriteria<D> query, Map queryArgs) {
        datastoreClient.proxy(query.toQuery(queryArgs))
    }
}
