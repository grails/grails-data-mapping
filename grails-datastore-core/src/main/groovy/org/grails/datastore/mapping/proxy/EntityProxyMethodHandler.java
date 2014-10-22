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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.datastore.mapping.proxy;

import java.lang.reflect.Method;

public abstract class EntityProxyMethodHandler extends GroovyObjectMethodHandler {

    public EntityProxyMethodHandler(Class<?> proxyClass) {
        super(proxyClass);
    }

    @Override
    protected Object getPropertyBeforeResolving(Object self, String property) {
        if (property.equals("proxy")) {
            return true;
        } else if (property.equals("proxyKey") || property.equals("id")) {
            return getProxyKey(self);
        } else if (property.equals("initialized")) {
            return isProxyInitiated(self);
        } else if (property.equals("target")) {
            return getProxyTarget(self);
        } else {
            return super.getPropertyBeforeResolving(self, property);
        }
    }
    
    @Override
    public Object invokeMethodBeforeResolving(Object self, String methodName, Object[] args) {
        Object result = invokeEntityProxyMethods(self, methodName, args);
        if(!wasHandled(result)) {
            return super.invokeMethodBeforeResolving(self, methodName, args);
        } else { 
            return result;
        }
    }
    
    @Override
    public Object handleInvocation(Object self, Method thisMethod, Object[] args) {
        Object result = invokeEntityProxyMethods(self, thisMethod.getName(), args);
        if(!wasHandled(result)) {
            result = super.handleInvocation(self, thisMethod, args);
            if(!wasHandled(result)) {
                return handleInvocationFallback(self, thisMethod, args);
            }
        }
        return result;
    }

    protected Object handleInvocationFallback(Object self, Method thisMethod, Object[] args) {
       return INVOKE_IMPLEMENTATION;
    }    
    
    protected Object invokeEntityProxyMethods(Object self, String methodName, Object[] args) {
        if (methodName.equals("isProxy")) {
            return true;
        } else if (methodName.equals("getProxyKey") || methodName.equals("getId")) {
            return getProxyKey(self);
        } else if (methodName.equals("isInitialized")) {
            return isProxyInitiated(self);
        } else if (methodName.equals("getTarget")) {
            return getProxyTarget(self);
        } else if (methodName.equals("initialize")) {
            initializeProxyTarget(self);
            return Void.class;
        } else {
            return INVOKE_IMPLEMENTATION;
        }
    }

    protected void initializeProxyTarget(Object self) {
        getProxyTarget(self);
    }

    protected Object getProxyTarget(Object self) {
        return resolveDelegate(self);
    }

    protected abstract Object isProxyInitiated(Object self);

    protected abstract Object getProxyKey(Object self);
}
