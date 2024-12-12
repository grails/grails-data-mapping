package org.grails.datastore.rx.collection

import grails.gorm.rx.collection.ObservableCollection
import groovy.transform.CompileStatic
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Consumer
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

/**
 * A trait that can be implemented by collection types to make them observable
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
trait RxCollection<T> implements ObservableCollection<T>{

    /**
     * The underlying observable
     */
    Observable observable

    /**
     * @return A list observable
     */
    Observable<List> toListObservable() {
        observable.toList().toObservable();
    }

    Observable toObservable() {
        return this.observable
    }

    Disposable subscribe(Consumer subscriber) {
        return observable.subscribe(subscriber)
    }
}