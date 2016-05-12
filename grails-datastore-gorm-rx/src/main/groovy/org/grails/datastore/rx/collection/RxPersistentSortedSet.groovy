package org.grails.datastore.rx.collection

import grails.gorm.rx.collection.RxPersistentCollection
import grails.gorm.rx.collection.RxUnidirectionalCollection
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.datastore.mapping.collection.PersistentSet
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.rx.RxDatastoreClient
import org.grails.datastore.rx.internal.RxDatastoreClientImplementor
import org.grails.datastore.rx.query.QueryState
import org.grails.datastore.rx.query.RxQuery
import rx.Observable
import rx.Subscriber
import rx.Subscription

/**
 * Represents a reactive sorted set that can be observed in order to allow non-blocking lazy loading of associations
 *
 * @author Graeme Rocher
 * @since 6.0
 */

@CompileStatic
@Slf4j
class RxPersistentSortedSet extends PersistentSet implements SortedSet,RxPersistentCollection, RxUnidirectionalCollection{
    final RxDatastoreClient datastoreClient
    final Association association

    Observable observable
    private QueryState queryState

    RxPersistentSortedSet( RxDatastoreClient datastoreClient, Association association, Serializable associationKey, QueryState queryState = null) {
        super(association, associationKey, null, new TreeSet())
        this.datastoreClient = datastoreClient
        this.association = association
        this.queryState = queryState
    }

    RxPersistentSortedSet( RxDatastoreClient datastoreClient, Association association, List<Serializable> entitiesKeys, QueryState queryState = null) {
        super(entitiesKeys, association.associatedEntity.javaClass, null)
        this.datastoreClient = datastoreClient
        this.association = association
        this.queryState = queryState
    }

    @Override
    void initialize() {
        if(initializing != null) return
        initializing = true


        try {
            def observable = toListObservable()

            log.warn("Association $association initialised using blocking operation. Consider using subscribe(..) or an eager query instead")

            addAll observable.toBlocking().first()
        } finally {
            initializing = false
            initialized = true
        }
    }

    @Override
    Observable<List> toListObservable() {
        toObservable().toList()
    }

    @Override
    Observable toObservable() {
        if(observable == null) {
            def query = ((RxDatastoreClientImplementor)datastoreClient).createQuery(childType, queryState)
            if(associationKey != null) {
                query.eq( association.inverseSide.name, associationKey )
            }
            else {
                query.in(association.associatedEntity.identity.name, keys.toList())
            }
            observable = ((RxQuery)query).findAll()
        }
        return observable
    }

    @Override
    Subscription subscribe(Subscriber subscriber) {
        return toObservable().subscribe(subscriber)
    }

    @Override
    Comparator comparator() {
        return ((SortedSet)collection).comparator()
    }

    @Override
    SortedSet subSet(Object fromElement, Object toElement) {
        return ((SortedSet)collection).subSet(fromElement, toElement)
    }

    @Override
    SortedSet headSet(Object toElement) {
        return ((SortedSet)collection).headSet(toElement)
    }

    @Override
    SortedSet tailSet(Object fromElement) {
        return ((SortedSet)collection).tailSet(fromElement)
    }

    @Override
    Object first() {
        return ((SortedSet)collection).first()
    }

    @Override
    Object last() {
        return ((SortedSet)collection).last()
    }

    @Override
    List<Serializable> getAssociationKeys() {
        if(keys != null) {
            return keys.toList()
        }
        else {
            return Collections.emptyList()
        }
    }
}
