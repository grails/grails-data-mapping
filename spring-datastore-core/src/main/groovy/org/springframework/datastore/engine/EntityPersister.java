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
package org.springframework.datastore.engine;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import org.springframework.datastore.core.Session;
import org.springframework.datastore.mapping.MappingContext;
import org.springframework.datastore.mapping.PersistentEntity;
import org.springframework.datastore.proxy.EntityProxy;
import org.springframework.datastore.reflect.ReflectionUtils;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A Persister specified to PersistentEntity instances
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public abstract class EntityPersister implements Persister, EntityInterceptorAware {
    private PersistentEntity persistentEntity;
    private MappingContext mappingContext;
    protected List<EntityInterceptor> interceptors = new ArrayList<EntityInterceptor>();
    protected Session session;
    private static final Map<Class, Class > PROXY_FACTORIES = new ConcurrentHashMap<Class, Class >();

    public EntityPersister(MappingContext mappingContext, PersistentEntity entity, Session session) {
        this.persistentEntity = entity;
        this.mappingContext = mappingContext;
        this.session = session;
    }

    public void addEntityInterceptor(EntityInterceptor interceptor) {
        if(interceptor != null) {
            this.interceptors.add(interceptor);
        }
    }

    public void setEntityInterceptors(List<EntityInterceptor> interceptors) {
        if(interceptors!=null) this.interceptors = interceptors;
    }

    /**
     * @return The MappingContext instance
     */
    public MappingContext getMappingContext() {
        return mappingContext;
    }

    /**
     * @return The PersistentEntity instance
     */
    public PersistentEntity getPersistentEntity() {
        return persistentEntity;
    }

    public Class getType() {
        return persistentEntity.getJavaClass();
    }

    /**
     * Obtains an objects identifer
     * @param obj The object
     * @return The identifier or null if it doesn't have one
     */
    public Serializable getObjectIdentifier(Object obj) {
        return (Serializable) new EntityAccess(getPersistentEntity(), obj).getIdentifier();
    }
    /**
     * Persists an object returning the identifier
     *
     * @param obj The object to persist
     * @return The identifer
     */
    public final Serializable persist(Object obj) {
        if(!persistentEntity.isInstance(obj)) throw new IllegalArgumentException("Object ["+obj+"] is not an instance supported by the persister for class ["+getType().getName()+"]");

        return persistEntity(getPersistentEntity(), new EntityAccess(getPersistentEntity(), obj));
    }

    public List<Serializable> persist(Iterable objs) {
        return persistEntities(getPersistentEntity(), objs);
    }

    public List<Object> retrieveAll(Iterable<Serializable> keys) {
        return retrieveAllEntities(getPersistentEntity(), keys);
    }

    public List<Object> retrieveAll(Serializable[] keys) {
        return retrieveAllEntities(getPersistentEntity(), keys);
    }

    protected abstract List<Object> retrieveAllEntities(PersistentEntity persistentEntity, Serializable[] keys);

    protected abstract List<Object> retrieveAllEntities(PersistentEntity persistentEntity, Iterable<Serializable> keys);

    protected abstract List<Serializable> persistEntities(PersistentEntity persistentEntity, Iterable objs);

    public final Object retrieve(Serializable key) {
        if(key == null) return null;
        return retrieveEntity(getPersistentEntity(), key);
    }

    /**
     * Retrieve a PersistentEntity for the given mappingContext and key
     *
     * @param persistentEntity The entity
     * @param key The key
     * @return The object or null if it doesn't exist
     */
    protected abstract Object retrieveEntity(PersistentEntity persistentEntity, Serializable key);

    /**
     * Persist the given persistent entity
     *
     * @param persistentEntity The PersistentEntity
     * @param entityAccess An object that allows easy access to the entities properties
     * @return The generated key
     */
    protected abstract Serializable persistEntity(PersistentEntity persistentEntity, EntityAccess entityAccess);

    public final void delete(Iterable objects) {
        if(objects != null) {
            deleteEntities(getPersistentEntity(), objects);
        }
    }

    public void delete(Object obj) {
        if(obj != null) {
            deleteEntity(getPersistentEntity(), obj);
        }
    }

    protected abstract void deleteEntity(PersistentEntity persistentEntity, Object obj);

    protected abstract void deleteEntities(PersistentEntity persistentEntity, Iterable objects);

    protected Object createProxiedInstance(final Class cls, Class proxyClass, final Serializable id) {
        MethodHandler mi = new MethodHandler() {
            private Object target;
            public Object invoke(Object proxy, Method method, Method proceed, Object[] args) throws Throwable {
                if(args.length == 0) {
                    final String methodName = method.getName();
                    if(methodName.equals("getId")) {
                        return id;
                    }
                    if(methodName.equals("initialize")) {
                        initialize();
                        return null;
                    }
                    if(methodName.equals("isInitialized")) {
                        return target != null;
                    }
                    if(methodName.equals("getTarget")) {
                        initialize();
                        return null;
                    }
                }
                if(target == null) initialize();
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

    protected Object getProxyInstance(Class type, Serializable id) {
        Class proxyClass = getProxyClass(type);        
        return createProxiedInstance(type, proxyClass, id);
    }

    protected Class getProxyClass(Class type) {

        Class proxyClass = PROXY_FACTORIES.get(type);
        if(proxyClass == null) {
            ProxyFactory pf = new ProxyFactory();
            pf.setSuperclass(type);
            pf.setInterfaces(new Class[]{ EntityProxy.class });
            final List excludes = new ArrayList() {{
                add("getMetaClass");
                add("metaClass");
                add("setMetaClass");
                add("invokeMethod");
                add("getProperty");
                add("setProperty");
                add("$getStaticMetaClass");
            }};
            pf.setFilter(new MethodFilter() {
                public boolean isHandled(Method method) {
                    final String methodName = method.getName();
                    if(methodName.indexOf("super$") > -1) {
                        return false;
                    }
                    else if(method.getParameterTypes().length == 0 && (methodName.equals("finalize"))) {
                        return false;
                    }
                    else if(excludes.contains(methodName) || method.isSynthetic() || method.isBridge()) {
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

