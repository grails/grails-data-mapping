/* Copyright (C) 2010 SpringSource
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.datastore.mapping.proxy;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;

import org.grails.datastore.mapping.core.Session;
import org.grails.datastore.mapping.reflect.ReflectionUtils;

/**
 * A proxy factory that uses Javassist to create proxies
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class JavassistProxyFactory implements org.grails.datastore.mapping.proxy.ProxyFactory {

    private static final Map<Class, Class > PROXY_FACTORIES = new ConcurrentHashMap<Class, Class >();

    private static final List EXCLUDES = Arrays.asList(
            "getMetaClass",
            "metaClass",
            "setMetaClass",
            "invokeMethod",
            "getProperty",
            "setProperty",
            "$getStaticMetaClass");

    public boolean isProxy(Object object) {
        return object instanceof EntityProxy;
    }

    public Serializable getIdentifier(Object obj) {
        return ((EntityProxy)obj).getProxyKey();

    }

    public <T> T createProxy(Session session, Class<T> type, Serializable key) {
        return (T) getProxyInstance(session, type, key);
    }

    protected Object createProxiedInstance(final Session session, final Class cls, Class proxyClass, final Serializable id) {
        MethodHandler mi = new MethodHandler() {
            private Object target;
            public Object invoke(Object proxy, Method method, Method proceed, Object[] args) throws Throwable {
                if (args.length == 0) {
                    final String methodName = method.getName();
                    if (methodName.equals("getId") || methodName.equals("getProxyKey")) {
                        return id;
                    }
                    if (methodName.equals("initialize")) {
                        initialize();
                        return null;
                    }
                    if (methodName.equals("isInitialized")) {
                        return target != null;
                    }
                    if (methodName.equals("getTarget")) {
                        initialize();
                        return null;
                    }
                }
                if (target == null) initialize();
                return org.springframework.util.ReflectionUtils.invokeMethod(method, target, args);
            }

            public void initialize() {
                target = session.retrieve(cls, id);
            }
        };
        Object proxy = ReflectionUtils.instantiate(proxyClass);
        ((ProxyObject)proxy).setHandler(mi);
        return proxy;
    }

    protected Object getProxyInstance(Session session, Class type, Serializable id) {
        Class proxyClass = getProxyClass(type);
        return createProxiedInstance(session, type, proxyClass, id);
    }

    protected Class getProxyClass(Class type) {

        Class proxyClass = PROXY_FACTORIES.get(type);
        if (proxyClass == null) {
            javassist.util.proxy.ProxyFactory pf = new ProxyFactory();
            pf.setSuperclass(type);
            pf.setInterfaces(new Class[]{ EntityProxy.class });
            pf.setFilter(new MethodFilter() {
                public boolean isHandled(Method method) {
                    final String methodName = method.getName();
                    if (methodName.indexOf("super$") > -1) {
                        return false;
                    }
                    if (method.getParameterTypes().length == 0 && (methodName.equals("finalize"))) {
                        return false;
                    }
                    if (EXCLUDES.contains(methodName) || method.isSynthetic() || method.isBridge()) {
                        return false;
                    }
                    return true;
                }
            });
            proxyClass = pf.createClass();
            PROXY_FACTORIES.put(type, proxyClass);
        }
        return proxyClass;
    }
}
