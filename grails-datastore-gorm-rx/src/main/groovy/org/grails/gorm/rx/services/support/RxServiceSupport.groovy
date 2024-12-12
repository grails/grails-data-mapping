package org.grails.gorm.rx.services.support

import groovy.transform.CompileStatic
import io.reactivex.rxjava3.annotations.NonNull
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableEmitter
import io.reactivex.rxjava3.core.ObservableOnSubscribe
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
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
        Scheduler scheduler = Schedulers.io()
        create(scheduler, callable)
    }

    /**
     * Create an observable from the given callable on the given Scheduler
     *
     * @param callable The callable
     * @return The {@link Observable}
     */
    static lockObj = new Object()
    static <T> Observable<T> create(Scheduler scheduler, Callable<T> callable) {
        synchronized(lockObj) {
            File testFile = new File("/tmp/gorm-rx.log")
            String input = testFile.text
            def os = testFile.newOutputStream()
            os << input
            os << "Creating observable ${callable.class.name}\n"
            os.flush()
            os.close()
        }

        Observable.create(new ObservableOnSubscribe<T>() {
            @Override
            void subscribe(@NonNull ObservableEmitter<T> emitter) throws Exception {

                Object result = callable.call()
                if(result instanceof Iterable) {
                    for(Object o in (Iterable)result) {
                        emitter.onNext((T)o)
                    }
                }
                else {
                    emitter.onNext(result)
                }
                emitter.onComplete()
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
        Single.fromCallable { callable.call() }.observeOn(scheduler)
    }
}
