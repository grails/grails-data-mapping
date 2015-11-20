/*
 * Copyright 2015 original authors
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

import org.grails.datastore.mapping.core.Session;
import org.grails.datastore.mapping.engine.AssociationQueryExecutor;
import org.grails.datastore.mapping.reflect.FieldEntityAccess;
import org.springframework.cglib.reflect.FastClass;
import org.springframework.cglib.reflect.FastMethod;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.util.ReflectionUtils;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * A proxy handler that uses a {@link org.grails.datastore.mapping.engine.AssociationQueryExecutor} to retrieve the association
 *
 * @author Graeme Rocher
 * @since 5.0
 */
public class AssociationQueryProxyHandler  extends EntityProxyMethodHandler {

    protected final Session session;
    protected final AssociationQueryExecutor executor;
    protected final Serializable associationKey;
    protected final FastClass fastClass;
    protected Object target;

    public AssociationQueryProxyHandler(Session session, AssociationQueryExecutor executor, Serializable associationKey) {
        super(executor.getIndexedEntity().getJavaClass());
        this.session = session;
        this.executor = executor;
        this.fastClass = session.getMappingContext().getEntityReflector(executor.getIndexedEntity()).fastClass();
        this.associationKey = associationKey;
    }

    @Override
    protected Object isProxyInitiated(Object self) {
        return target != null;
    }

    @Override
    protected Object getProxyKey(Object self) {
        return associationKey;
    }


    @Override
    protected Object resolveDelegate(Object self) {
        if (target == null) {
            final List results = executor.query(associationKey);
            if(executor.doesReturnKeys()) {
                if(!results.isEmpty()) {
                    target = session.retrieve(executor.getIndexedEntity().getJavaClass(), (Serializable) results.get(0));
                }
            }
            else {
                if(!results.isEmpty()) {
                    target = results.get(0);
                }
            }

            // This tends to happen during unit testing if the proxy class is not properly mocked
            // and therefore can't be found in the session.
            if( target == null ) {
                throw new DataIntegrityViolationException("Proxy for ["+ proxyClass.getName()+"] for association ["+executor.getIndexedEntity().getName()+"] could not be initialized");
            }
        }
        return target;
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
