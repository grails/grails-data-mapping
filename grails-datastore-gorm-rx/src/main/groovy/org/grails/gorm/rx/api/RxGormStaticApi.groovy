package org.grails.gorm.rx.api

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.finders.DynamicFinder
import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.query.Query
import org.grails.datastore.rx.RxDatastoreClient
import org.grails.datastore.rx.query.RxQuery
import org.grails.gorm.rx.finders.CountByFinder
import org.grails.gorm.rx.finders.FindAllByBooleanFinder
import org.grails.gorm.rx.finders.FindAllByFinder
import org.grails.gorm.rx.finders.FindByBooleanFinder
import org.grails.gorm.rx.finders.FindByFinder
import org.grails.gorm.rx.finders.FindOrCreateByFinder
import org.grails.gorm.rx.finders.FindOrSaveByFinder
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

    protected final List<FinderMethod> gormDynamicFinders

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
