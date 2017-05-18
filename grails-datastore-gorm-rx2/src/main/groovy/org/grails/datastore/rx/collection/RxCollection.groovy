package org.grails.datastore.rx.collection

import grails.gorm.rx.collection.ObservableCollection
import groovy.transform.CompileStatic
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.Observer

/**
 * A trait that can be implemented by collection types to make them observable
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
trait RxCollection implements ObservableCollection {

    /**
     * The underlying observable
     */
    Observable observable

    /**
     * @return A list observable
     */
    Single<List> toListObservable() {
        observable.toList()
    }

    Observable toObservable() {
        return this.observable
    }

    void subscribe(Observer observer) {
        observable.subscribe(observer)
    }
}