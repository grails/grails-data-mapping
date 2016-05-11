package org.grails.datastore.rx.proxy;

import org.grails.datastore.mapping.proxy.EntityProxyMethodHandler;
import org.grails.datastore.mapping.reflect.FieldEntityAccess;
import org.grails.datastore.rx.RxDatastoreClient;
import org.grails.datastore.rx.query.QueryState;
import org.springframework.cglib.reflect.FastClass;
import org.springframework.cglib.reflect.FastMethod;
import org.springframework.util.ReflectionUtils;
import rx.Observable;
import rx.Subscriber;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Abstract proxy generator for ObservableProxy instances
 *
 * @author Graeme Rocher
 * @since 6.0
 */
public abstract class AbstractObservableProxyMethodHandler extends EntityProxyMethodHandler {
    protected final Class type;
    protected final RxDatastoreClient client;
    protected final FastClass fastClass;
    protected final QueryState queryState;
    protected Object target;

    public AbstractObservableProxyMethodHandler(Class<?> proxyClass, Class type, QueryState queryState, RxDatastoreClient client) {
        super(proxyClass);
        this.type = type;
        this.queryState = queryState;
        this.client = client;
        this.fastClass = FieldEntityAccess.getOrIntializeReflector(client.getMappingContext().getPersistentEntity(type.getName())).fastClass();
    }

    @Override
    protected Object isProxyInitiated(Object self) {
        return target != null;
    }

    protected abstract Observable resolveObservable();

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
            ReflectionUtils.handleReflectionException(e);
        }
        return null;
    }
}
