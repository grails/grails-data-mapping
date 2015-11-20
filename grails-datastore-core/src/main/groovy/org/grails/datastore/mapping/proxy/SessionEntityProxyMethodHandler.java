/*
 * Copyright 2014 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.grails.datastore.mapping.proxy;

import org.grails.datastore.mapping.core.Session;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.reflect.FieldEntityAccess;
import org.springframework.cglib.reflect.FastClass;
import org.springframework.cglib.reflect.FastMethod;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.util.ReflectionUtils;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
* An {@link EntityProxyMethodHandler} that uses the session to initialize a target for the given id
*
* @author Lari Hotari
* @author Graeme Rocher
*/
public class SessionEntityProxyMethodHandler extends EntityProxyMethodHandler {
    private final Session session;
    private final Class cls;
    private final Serializable id;
    protected final FastClass fastClass;
    protected Object target;


    public SessionEntityProxyMethodHandler(Class proxyClass, Session session, Class cls, Serializable id) {
        super(proxyClass);
        this.session = session;
        this.cls = cls;
        this.id = id;
        final PersistentEntity entity = session.getMappingContext().getPersistentEntity(proxyClass.getName());
        this.fastClass = FieldEntityAccess.getOrIntializeReflector(entity).fastClass();
    }

    @Override
    protected Object resolveDelegate(Object self) {
        if (target == null) {
            initializeTarget();

            // This tends to happen during unit testing if the proxy class is not properly mocked
            // and therefore can't be found in the session.
            if( target == null ) {
                throw new DataIntegrityViolationException("Proxy for ["+ cls.getName()+":"+ id +"] could not be initialized");
            }
        }
        return target;
    }

    protected void initializeTarget() {
        target = session.retrieve(cls, id);
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
