package org.grails.datastore.rx.proxy;

import org.grails.datastore.mapping.query.Query;
import org.grails.datastore.rx.RxDatastoreClient;
import org.grails.datastore.rx.query.QueryState;
import org.grails.datastore.rx.query.RxQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;

import java.io.Serializable;

/**
 * A proxy {@link javassist.util.proxy.MethodHandler} that uses a query to resolve the target
 *
 * @author Graeme Rocher
 * @since 6.0
 */
public class QueryObservableProxyMethodHandler extends AbstractObservableProxyMethodHandler {
    private static final Logger LOG = LoggerFactory.getLogger(QueryObservableProxyMethodHandler.class);

    private final Query query;
    protected Observable observable;
    protected Serializable proxyKey;

    public QueryObservableProxyMethodHandler(Class proxyClass, Query query, QueryState queryState, RxDatastoreClient client) {
        super(proxyClass, query.getEntity().getJavaClass(), queryState, client);
        this.query = query;
    }

    @Override
    protected Observable resolveObservable() {
        query.projections().id();
        Observable queryResult = ((RxQuery) query).singleResult();

        this.observable = queryResult.switchMap(new Func1<Serializable, Observable>() {
            @Override
            public Observable call(Serializable id) {
                if(id != null) {
                    proxyKey = id;
                    Object loadedEntity = queryState.getLoadedEntity(type, id);
                    if(loadedEntity != null) {
                        return Observable.just(loadedEntity);
                    }
                    else {
                        return client.get(type, id);
                    }
                }
                else {
                    return null;
                }
            }
        });
        this.observable.subscribe(new Action1() {
            @Override
            public void call(Object o) {
                target = o;
            }
        });
        return this.observable;
    }

    @Override
    protected Object resolveDelegate(Object self) {
        if(target != null) {
            return target;
        }

        Observable observable = resolveObservable();
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
