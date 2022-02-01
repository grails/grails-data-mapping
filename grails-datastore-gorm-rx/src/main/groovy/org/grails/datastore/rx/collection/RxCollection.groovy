package org.grails.datastore.rx.collection

import grails.gorm.rx.collection.ObservableCollection
import groovy.transform.CompileStatic
import groovy.transform.Generated
import rx.Observable
import rx.Subscriber
import rx.Subscription

/**
 * A trait that can be implemented by collection types to make them observable
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
trait RxCollection implements ObservableCollection{

    /**
     * The underlying observable
     */
    Observable observable

    /**
     * @return A list observable
     */
    @Generated
    Observable<List> toListObservable() {
        observable.toList()
    }

    @Generated
    Observable toObservable() {
        return this.observable
    }

    @Generated
    Subscription subscribe(Subscriber subscriber) {
        return observable.subscribe(subscriber)
    }
}