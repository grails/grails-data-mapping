package org.grails.gorm.rx.finders

import grails.gorm.rx.RxEntity
import groovy.transform.CompileStatic
import io.reactivex.rxjava3.annotations.NonNull
import io.reactivex.rxjava3.core.ObservableEmitter
import io.reactivex.rxjava3.core.ObservableOnSubscribe
import io.reactivex.rxjava3.schedulers.Schedulers
import org.grails.datastore.gorm.finders.DynamicFinderInvocation
import org.grails.datastore.gorm.finders.MethodExpression
import org.grails.datastore.rx.RxDatastoreClient
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer;
import org.reactivestreams.Subscriber

/**
 * Implementation of findOrCreateBy* finder for RxGORM
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class FindOrCreateByFinder extends FindByFinder {
    FindOrCreateByFinder(RxDatastoreClient datastoreClient) {
        super(datastoreClient)
        setPattern(org.grails.datastore.gorm.finders.FindOrCreateByFinder.METHOD_PATTERN)
    }

    @Override
    protected Object doInvokeInternal(DynamicFinderInvocation invocation) {
        Observable observable = (Observable)super.doInvokeInternal(invocation)
        observable.switchIfEmpty(Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            void subscribe(@NonNull ObservableEmitter<Object> emitter) throws Exception {
                Map m = [:]
                List<MethodExpression> expressions = invocation.getExpressions()
                for (MethodExpression me in expressions) {
                    if (!(me instanceof MethodExpression.Equal)) {
                        throw new MissingMethodException(invocation.methodName, invocation.javaClass, invocation.arguments)
                    }
                    String propertyName = me.propertyName
                    Object[] arguments = me.getArguments()
                    m.put(propertyName, arguments[0])
                    def newInstance = invocation.javaClass.newInstance(m)
                    if(shouldSaveOnCreate()) {
                        def saveObservable = ((RxEntity) newInstance).save()
                        saveObservable.observeOn(Schedulers.io())
                        emitter.onNext(saveObservable.blockingFirst())
                    }
                    else {
                        emitter.onNext newInstance
                    }
                    emitter.onComplete()
                }

            }
        }));

    }

    protected boolean shouldSaveOnCreate() {
        return false
    }
}
