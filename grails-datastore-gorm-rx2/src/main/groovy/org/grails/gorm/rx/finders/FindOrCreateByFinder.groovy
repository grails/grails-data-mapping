package org.grails.gorm.rx.finders

import grails.gorm.rx.RxEntity
import groovy.transform.CompileStatic
import io.reactivex.ObservableEmitter
import io.reactivex.Observer
import io.reactivex.annotations.NonNull
import io.reactivex.disposables.Disposable
import org.grails.datastore.gorm.finders.DynamicFinderInvocation
import org.grails.datastore.gorm.finders.MethodExpression
import org.grails.datastore.rx.RxDatastoreClient
import io.reactivex.Observable

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
        observable.switchIfEmpty(Observable.create( { ObservableEmitter s ->
                Thread.start {
                    Map m = [:]
                    List<MethodExpression> expressions = invocation.getExpressions()
                    for (MethodExpression me in expressions) {
                        if (!(me instanceof MethodExpression.Equal)) {
                            throw new MissingMethodException(invocation.methodName, invocation.javaClass, invocation.arguments)
                        }
                        String propertyName = me.propertyName
                        Object[] arguments = me.getArguments()
                        m.put(propertyName, arguments[0])
                    }

                    def newInstance = invocation.javaClass.newInstance(m)
                    if(shouldSaveOnCreate()) {
                        def saveObservable = ((RxEntity) newInstance).save()
                        saveObservable.subscribe(new Observer(){

                            @Override
                            void onError(Throwable e) {
                                s.onError(e)
                            }

                            @Override
                            void onComplete() {
                                s.onComplete()
                            }

                            @Override
                            void onSubscribe(@NonNull Disposable d) {
                                //no-op
                            }

                            @Override
                            void onNext(Object o) {
                                s.onNext(o)
                            }
                        })
                    }
                    else {
                        s.onNext newInstance
                    }
                }

            })
        )
    }

    protected boolean shouldSaveOnCreate() {
        return false
    }
}
