package org.grails.gorm.rx.api

import grails.gorm.rx.CriteriaBuilder
import grails.gorm.rx.DetachedCriteria
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.codehaus.groovy.runtime.InvokerHelper
import org.grails.datastore.gorm.finders.DynamicFinder
import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.core.SessionCallback
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.query.Query
import org.grails.datastore.mapping.query.api.BuildableCriteria
import org.grails.datastore.mapping.query.api.Criteria
import org.grails.datastore.rx.RxDatastoreClient
import org.grails.datastore.rx.query.RxQuery
import org.grails.gorm.rx.finders.CountByFinder
import org.grails.gorm.rx.finders.FindAllByBooleanFinder
import org.grails.gorm.rx.finders.FindAllByFinder
import org.grails.gorm.rx.finders.FindByBooleanFinder
import org.grails.gorm.rx.finders.FindByFinder
import org.grails.gorm.rx.finders.FindOrCreateByFinder
import org.grails.gorm.rx.finders.FindOrSaveByFinder
import org.springframework.beans.PropertyAccessorFactory
import rx.Observable

/**
 * Bridge to the implementation of the static method level operations for RX GORM
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class RxGormStaticApi<D> {

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

    Observable<D> get(Serializable id) {
        datastoreClient.get(entity.javaClass, id)
    }

    Observable<Integer> count() {
        def query = datastoreClient.createQuery(entity.javaClass)
        query.projections().count()
        return ((RxQuery)query).singleResult()
    }

    Observable<D> list(Map params = Collections.emptyMap()) {
        def query = datastoreClient.createQuery(entity.javaClass)
        DynamicFinder.populateArgumentsForCriteria(entity.javaClass, query, params)
        return ((RxQuery<D>) query).findAll()
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
    CriteriaBuilder createCriteria() {
        new CriteriaBuilder(persistentClass, datastoreClient, datastoreClient.mappingContext)
    }

    /**
     * Creates a criteria builder instance
     */

    Observable withCriteria(@DelegatesTo(Criteria) Closure callable) {
        (Observable)InvokerHelper.invokeMethod(createCriteria(), 'call', callable)
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
         new FindAllByBooleanFinder(datastoreClient)]
    }

}
