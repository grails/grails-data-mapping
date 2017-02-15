package org.grails.gorm.rx.services.support

import groovy.transform.CompileStatic
import rx.Observable
import rx.Single
import rx.SingleSubscriber
import rx.Subscriber
import rx.schedulers.Schedulers

import java.util.concurrent.Callable

/**
 * Helper class for creating observables that run of the IO scheduler for blocking GORM operations
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
class RxServiceSupport {

    /**
     * Create an observable from the given callable
     *
     * @param callable The callable
     * @return The {@link Observable}
     */
    static <T> Observable<T> create(Callable<T> callable) {
        Observable.create({ Subscriber<? super T> subscriber ->
            subscriber.onStart()
            try {
                def result = callable.call()
                if(result instanceof Iterable) {
                    for(o in result) {
                        subscriber.onNext(result)
                    }
                }
                else {
                    subscriber.onNext(result)
                }
                subscriber.onCompleted()
            } catch (Throwable e) {
                subscriber.onError(e)
            }
        } as Observable.OnSubscribe).observeOn(Schedulers.io())
    }

    /**
     * Create an observable from the given callable
     *
     * @param callable The callable
     * @return The {@link Observable}
     */
    static <T> Single<T> createSingle(Callable<T> callable) {
        Single.create({ SingleSubscriber<? super T> singleSubscriber ->
            try {
                singleSubscriber.onSuccess(callable.call())
            } catch (Throwable e) {
                singleSubscriber.onError(e)
            }
        } as Single.OnSubscribe).observeOn(Schedulers.io())
    }
}
