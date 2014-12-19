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

import groovy.lang.GroovyObject;
import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import org.grails.datastore.mapping.core.Session;
import org.grails.datastore.mapping.reflect.ReflectionUtils;
import org.springframework.dao.DataIntegrityViolationException;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A proxy factory that uses Javassist to create proxies
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class JavassistProxyFactory implements org.grails.datastore.mapping.proxy.ProxyFactory {

    private static final Map<Class, Class > PROXY_FACTORIES = new ConcurrentHashMap<Class, Class >();
    private static final Map<Class, Class > ID_TYPES = new ConcurrentHashMap<Class, Class >();
    private static final Class[] EMPTY_CLASS_ARRAY = {};

    private static final Set<String> EXCLUDES = new HashSet(Arrays.asList("$getStaticMetaClass"));

    public boolean isProxy(Object object) {
        return (object instanceof EntityProxy) || (object instanceof ProxyObject);
    }

    public Serializable getIdentifier(Object obj) {
        return ((EntityProxy)obj).getProxyKey();

    }

    /**
     * Checks whether a given proxy is initialized
     *
     * @param object The object to check
     * @return True if it is
     */
    public boolean isInitialized(Object object) {
        return !isProxy(object) || ((EntityProxy) object).isInitialized();
    }

    /**
     * Unwraps the given proxy if it is one
     *
     * @param object The object
     * @return The unwrapped proxy
     */
    public Object unwrap(Object object) {
        if (isProxy(object)) {
            return ((EntityProxy)object).getTarget();
        }
        return object;
    }

    public <T> T createProxy(Session session, Class<T> type, Serializable key) {
        return (T) getProxyInstance(session, type, key);
    }

    protected Object createProxiedInstance(final Session session, final Class cls, Class proxyClass, final Serializable id) {
        MethodHandler mi = new EntityProxyMethodHandler(proxyClass) {
            private Object target;
            
            @Override
            protected Object resolveDelegate(Object self) {
                if (target == null) {
                    target = session.retrieve(cls, id);

                    // This tends to happen during unit testing if the proxy class is not properly mocked
                    // and therefore can't be found in the session.
                    if( target == null ) {
                        throw new DataIntegrityViolationException("Proxy for ["+cls.getName()+":"+id+"] could not be initialized");
                    }
                }
                return target;
            }
            
            @Override
            protected Object isProxyInitiated(Object self) {
                return target != null;
            }
            
            @Override
            protected Object getProxyKey(Object self) {
                return id;
            }
            
            protected Object handleInvocationFallback(Object self, Method thisMethod, Object[] args) {
                Object actualTarget = getProxyTarget(self);
                if(!thisMethod.getDeclaringClass().isInstance(actualTarget)) {
                    if(Modifier.isPublic(thisMethod.getModifiers())) {
                        try {
                            thisMethod = actualTarget.getClass().getMethod(thisMethod.getName(), thisMethod.getParameterTypes());
                        } catch (Exception e) {
                            org.springframework.util.ReflectionUtils.handleReflectionException(e);
                        }
                    } else {
                        thisMethod = org.springframework.util.ReflectionUtils.findMethod(actualTarget.getClass(), thisMethod.getName(), thisMethod.getParameterTypes());
                    }
                }
                return org.springframework.util.ReflectionUtils.invokeMethod(thisMethod, actualTarget, args);
            }    
        };
        Object proxy = ReflectionUtils.instantiate(proxyClass);
        ((ProxyObject)proxy).setHandler(mi);
        return proxy;
    }

    protected Serializable convertId(Serializable idAsInput, Class<?> ownerClass) {
        if(idAsInput==null) return null;
        Class<?> idType = ID_TYPES.get(ownerClass);
        if(idType != null) {
            if(idType.isInstance(idAsInput)) {
                return idAsInput;
            }
            if(idType == String.class) {
                return idAsInput.toString();
            }
            if(Number.class.isAssignableFrom(idType) && idAsInput instanceof Number) {
                Number idNumber = (Number)idAsInput;
                if(idType==Integer.class) {
                    return idNumber.intValue();
                }
                if(idType==Long.class) {
                    return idNumber.longValue();
                }
            }
            if(idType==Integer.class) {
                return Integer.parseInt(idAsInput.toString());
            }
            if(idType==Long.class) {
                return Long.parseLong(idAsInput.toString());
            }
            return (Serializable)idType.cast(idAsInput);
        } else {
            return idAsInput;
        }
    }

    protected Object getProxyInstance(Session session, Class type, Serializable idAsInput) {
        Class proxyClass = getProxyClass(type);
        final Serializable id = convertId(idAsInput, type);
        return createProxiedInstance(session, type, proxyClass, id);
    }

    protected Class getProxyClass(Class type) {

        Class proxyClass = PROXY_FACTORIES.get(type);
        if (proxyClass == null) {
            javassist.util.proxy.ProxyFactory pf = new ProxyFactory();
            pf.setSuperclass(type);
            pf.setInterfaces(new Class[]{ EntityProxy.class, GroovyObject.class });
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
            
            Method getIdMethod = org.springframework.util.ReflectionUtils.findMethod(type, "getId", EMPTY_CLASS_ARRAY);
            Class<?> idType = getIdMethod.getReturnType();
            if(idType != null) {
                ID_TYPES.put(type, idType);
            }
        }
        return proxyClass;
    }
}
