package org.grails.gorm.rx.services.support

import groovy.transform.CompileStatic
import io.reactivex.Emitter
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.SingleOnSubscribe
import io.reactivex.annotations.NonNull
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers

import java.util.concurrent.Callable

/**
 * Helper class for creating observables that run of the IO scheduler for blocking GORM operations
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
class RxServiceSupport<T> {

    /**
     * Create an observable from the given callable
     *
     * @param callable The callable
     * @return The {@link Observable}
     */
    static <T> Observable<T> create(Callable<T> callable) {
        Scheduler scheduler = Schedulers.io()
        create(scheduler, callable)
    }

    /**
     * Create an observable from the given callable on the given Scheduler
     *
     * @param callable The callable
     * @return The {@link Observable}
     */
    static <T> Observable<T> create(Scheduler scheduler, Callable<T> callable) {
        
        Observable.generate(new Consumer<Emitter<T>>() {
            @Override
            void accept(@NonNull Emitter<T> tEmitter) throws Exception {
                def result = callable.call()
                if(result instanceof Iterable) {
                    result = ((Iterable)result).iterator()
                }

                if (result == null) {
                    tEmitter.onComplete()
                }
                else if (result instanceof Iterator) {
                    Iterator i = (Iterator)result
                    if(i.hasNext()) {
                        tEmitter.onNext((T)i.next())
                    }
                    else {
                        tEmitter.onComplete()
                    }
                }
                else {
                    tEmitter.onNext((T)result)
                    tEmitter.onComplete()
                }
            }
        }).observeOn(scheduler)
    }

    /**
     * Create an observable from the given callable
     *
     * @param callable The callable
     * @return The {@link Observable}
     */
    static <T> Single<T> createSingle(Callable<T> callable) {
        Scheduler scheduler = Schedulers.io()
        createSingle(scheduler, callable)
    }

    /**
     * Create an observable from the given callable on the given scheduler
     *
     * @param callable The callable
     * @return The {@link Observable}
     */
    static <T>  Single<T> createSingle(Scheduler scheduler, Callable<T> callable) {
        Single.create(new SingleOnSubscribe() {
            @Override
            void subscribe(@NonNull SingleEmitter emitter) throws Exception {
                try {
                    def result = callable.call()
                    emitter.onSuccess(result)
                } catch (Throwable e) {
                    emitter.onError(e)
                }
            }
        }).observeOn(scheduler)
    }
}
