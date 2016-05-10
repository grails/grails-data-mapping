package org.grails.datastore.rx.proxy;

import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.proxy.EntityProxyMethodHandler;
import org.grails.datastore.mapping.reflect.FieldEntityAccess;
import org.grails.datastore.rx.RxDatastoreClient;
import org.grails.datastore.rx.internal.RxDatastoreClientImplementor;
import org.grails.datastore.rx.query.QueryState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.reflect.FastClass;
import org.springframework.cglib.reflect.FastMethod;
import org.springframework.util.ReflectionUtils;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Created by graemerocher on 10/05/16.
 */
public class ObservableProxyMethodHandler extends EntityProxyMethodHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ObservableProxyMethodHandler.class);
    protected final Class type;
    protected final Serializable proxyKey;
    protected final RxDatastoreClient client;
    protected final FastClass fastClass;
    protected final QueryState queryState;
    protected Object target;
    protected Observable observable;

    public ObservableProxyMethodHandler(Class<?> proxyClass, Class type, Serializable proxyKey, RxDatastoreClient client, QueryState queryState) {
        super(proxyClass);
        this.type = type;
        this.queryState = queryState;
        this.proxyKey = proxyKey;
        this.client = client;
        final PersistentEntity entity = client.getMappingContext().getPersistentEntity(proxyClass.getName());
        this.fastClass = FieldEntityAccess.getOrIntializeReflector(entity).fastClass();

    }

    @Override
    protected Object isProxyInitiated(Object self) {
        return target != null;
    }

    @Override
    protected Object resolveDelegate(Object self) {
        if(target != null) {
            return target;
        }

        Object loadedEntity = queryState.getLoadedEntity(type, proxyKey);
        if(loadedEntity != null) {
            this.target = loadedEntity;
        }
        else {
            Observable observable = resolveObservable();
            if(LOG.isWarnEnabled()) {
                LOG.warn("Entity of type [{}] with id [{}] lazy loaded using a blocking operation. Consider using ObservableProxy.subscribe(..) instead", type.getName(), proxyKey);
            }
            this.target = observable.toBlocking().first();
        }
        return this.target;
    }

    private Observable resolveObservable() {
        this.observable = ((RxDatastoreClientImplementor) client).get(type, proxyKey, queryState);
        observable.subscribe(new Action1() {
            @Override
            public void call(Object o) {
                target = o;
            }
        });
        return observable;
    }

    @Override
    protected Object getProxyKey(Object self) {
        return proxyKey;
    }

    @Override
    protected Object invokeEntityProxyMethods(Object self, String methodName, Object[] args) {
        if(methodName.equals("subscribe")) {
            Observable observable = resolveObservable();
            return observable.subscribe((Subscriber) args[0]);
        }
        else if(methodName.equals("toObservable")) {
            return resolveObservable();
        }
        else {
            return super.invokeEntityProxyMethods(self, methodName, args);
        }
    }

    protected Object handleInvocationFallback(Object self, Method thisMethod, Object[] args) {
        Object actualTarget = getProxyTarget(self);
        FastMethod fastMethod;
        if(!thisMethod.getDeclaringClass().isInstance(actualTarget)) {
            if(Modifier.isPublic(thisMethod.getModifiers())) {
                try {
                    fastMethod = fastClass.getMethod(thisMethod);
                } catch (Exception e) {
                    fastMethod = null;
                    org.springframework.util.ReflectionUtils.handleReflectionException(e);
                }
            } else {
                final Method method = ReflectionUtils.findMethod(actualTarget.getClass(), thisMethod.getName(), thisMethod.getParameterTypes());
                fastMethod = fastClass.getMethod(method);
            }
        }
        else {
            fastMethod = fastClass.getMethod(thisMethod);
        }
        try {
            return fastMethod.invoke(actualTarget, args);
        } catch (InvocationTargetException e) {
            org.springframework.util.ReflectionUtils.handleReflectionException(e);
        }
        return null;
    }
}
