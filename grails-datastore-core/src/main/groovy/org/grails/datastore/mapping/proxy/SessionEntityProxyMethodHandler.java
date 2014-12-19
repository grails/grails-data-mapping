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
import org.springframework.dao.DataIntegrityViolationException;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
* Created by lari on 19/12/14.
*/
public class SessionEntityProxyMethodHandler extends EntityProxyMethodHandler {
    private final Session session;
    private final Class cls;
    private final Serializable id;
    protected Object target;

    public SessionEntityProxyMethodHandler(Class proxyClass, Session session, Class cls, Serializable id) {
        super(proxyClass);
        this.session = session;
        this.cls = cls;
        this.id = id;
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
}
