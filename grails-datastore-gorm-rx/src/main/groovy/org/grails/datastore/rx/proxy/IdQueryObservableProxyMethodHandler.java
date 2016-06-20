package org.grails.datastore.rx.proxy;

import org.grails.datastore.mapping.query.Query;
import org.grails.datastore.rx.RxDatastoreClient;
import org.grails.datastore.rx.internal.RxDatastoreClientImplementor;
import org.grails.datastore.rx.query.QueryState;
import org.grails.datastore.rx.query.RxQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Func1;

import java.io.Serializable;

/**
 * A proxy {@link javassist.util.proxy.MethodHandler} that uses a query to resolve the id of the the target
 *
 * @author Graeme Rocher
 * @since 6.0
 */
public class IdQueryObservableProxyMethodHandler extends AbstractObservableProxyMethodHandler {
    private static final Logger LOG = LoggerFactory.getLogger(IdQueryObservableProxyMethodHandler.class);

    private final Query query;
    protected final Observable observable;
    protected Serializable proxyKey;

    public IdQueryObservableProxyMethodHandler(Class proxyClass, Query query, QueryState queryState, RxDatastoreClient client) {
        super(proxyClass, query.getEntity().getJavaClass(), queryState, client);
        this.query = query;
        this.observable = resolveObservable();
    }

    @Override
    protected Observable resolveObservable() {
        query.projections().id();
        Observable queryResult = ((RxQuery) query).singleResult();

        queryResult = queryResult.switchMap(new Func1<Serializable, Observable>() {
            @Override
            public Observable call(Serializable id) {
                if(id != null) {
                    proxyKey = id;
                    Object loadedEntity = queryState.getLoadedEntity(type, id);
                    if(loadedEntity != null) {
                        return Observable.just(loadedEntity);
                    }
                    else {
                        return ((RxDatastoreClientImplementor)client).get(type, id, queryState);
                    }
                }
                else {
                    return null;
                }
            }
        });
        return queryResult.map(new Func1() {
            @Override
            public Object call(Object o) {
                target = o;
                return o;
            }
        });
    }

    @Override
    protected Object resolveDelegate(Object self) {
        if(target != null) {
            return target;
        }
        if(LOG.isWarnEnabled()) {
            LOG.warn("Entity of type [{}] lazy loaded using a blocking operation. Consider using ObservableProxy.subscribe(..) instead", type.getName());
        }
        this.target = observable.toBlocking().first();
        return this.target;
    }

    @Override
    protected Object getProxyKey(Object self) {
        resolveDelegate(self);
        return proxyKey;
    }
}
