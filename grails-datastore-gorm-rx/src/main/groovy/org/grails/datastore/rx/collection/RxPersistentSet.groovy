package org.grails.datastore.rx.collection

import grails.gorm.rx.collection.RxPersistentCollection
import grails.gorm.rx.collection.RxUnidirectionalCollection
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Consumer
import org.grails.datastore.mapping.collection.PersistentSet
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.query.Query
import org.grails.datastore.rx.RxDatastoreClient
import org.grails.datastore.rx.exceptions.BlockingOperationException
import org.grails.datastore.rx.internal.RxDatastoreClientImplementor
import org.grails.datastore.rx.query.QueryState
import org.grails.datastore.rx.query.RxQuery
import io.reactivex.rxjava3.core.Observable
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

/**
 * Represents a reactive set that can be observed in order to allow non-blocking lazy loading of associations
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
@Slf4j
class RxPersistentSet<T> extends PersistentSet implements RxPersistentCollection<T>, RxUnidirectionalCollection, RxCollection<T> {
    final RxDatastoreClient datastoreClient
    final Association association

    protected final QueryState queryState

    RxPersistentSet( RxDatastoreClient datastoreClient, Association association, Serializable associationKey, QueryState queryState = new QueryState()) {
        super(association, associationKey, null)
        this.datastoreClient = datastoreClient
        this.association = association
        this.queryState = queryState
        this.observable = resolveObservable()
    }

    RxPersistentSet( RxDatastoreClient datastoreClient, Association association, Serializable associationKey, Set target, QueryState queryState = new QueryState()) {
        super(association, associationKey, null, target)
        this.datastoreClient = datastoreClient
        this.association = association
        this.queryState = queryState
        this.observable = resolveObservable()
    }

    RxPersistentSet( RxDatastoreClient datastoreClient, Association association, List<Serializable> entitiesKeys, QueryState queryState = null) {
        super(entitiesKeys, association.associatedEntity.javaClass, null)
        this.datastoreClient = datastoreClient
        this.association = association
        this.queryState = queryState
        this.observable = resolveObservable()
    }

    RxPersistentSet( RxDatastoreClient datastoreClient, Association association, Query initializerQuery, QueryState queryState = null) {
        super(association, null, null)
        this.datastoreClient = datastoreClient
        this.association = association
        this.queryState = queryState
        this.observable = resolveObservable(initializerQuery)
    }


    protected Observable resolveObservable() {
        def query = ((RxDatastoreClientImplementor)datastoreClient).createQuery(childType, queryState)
        if(associationKey != null) {
            query.eq( association.inverseSide.name, associationKey )
        }
        else {
            query.in(association.associatedEntity.identity.name, keys.toList())
        }
        return resolveObservable(query)
    }

    protected Observable resolveObservable(Query query) {
        ((RxQuery) query).findAll()
    }

    @Override
    void initialize() {
        if(initializing != null) return
        initializing = Boolean.TRUE

        try {
            def observable = toListObservable()

            if(((RxDatastoreClientImplementor)datastoreClient).isAllowBlockingOperations()) {
                log.warn("Association $association initialised using blocking operation. Consider using subscribe(..) or an eager query instead")

                addAll observable.blockingFirst()
            }
            else {
                throw new BlockingOperationException("Cannot initialize $association using a blocking operation. Use subscribe(..) instead.")
            }
        } finally {
            initializing = false
            initialized = true
        }
    }


    @Override
    List<Serializable> getAssociationKeys() {
        if(keys != null) {
            return keys.toList() as List<Serializable>
        }
        else {
            return Collections.emptyList()
        }
    }
/**
 * A convenience method that subscribes to the Observable as provided by {@link #toObservable}.
 *
 * <p>
 * For more information on Subscriptions see the
 * <a href="http://reactivex.io/documentation/observable.html">ReactiveX documentation</a>.
 * </p>
 *
 * @param subscriber the Subscriber that will handle emissions and notifications from the Observable
 * @return a Subscription reference with which Subscribers that are Observers can
 *         unsubscribe from the Observable
 */

    @Override
    Disposable subscribe(Consumer<? super T> subscriber) {
        return null
    }
}
