/* Copyright (C) 2013 SpringSource
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
package org.grails.datastore.gorm.internal

import groovy.transform.CompileStatic

import org.apache.commons.lang.ArrayUtils
import org.codehaus.groovy.runtime.metaclass.MethodSelectionException

/**
 * Not public API. Used by GormEnhancer
 */
@SuppressWarnings("rawtypes")
@CompileStatic
class InstanceMethodInvokingClosure extends Closure {
    private String methodName
    private apiDelegate
    private Class[] parameterTypes
    private MetaMethod metaMethod
    
    InstanceMethodInvokingClosure(apiDelegate, Class<?> persistentClass, String methodName, Class[] parameterTypes) {
        super(apiDelegate, apiDelegate)
        this.apiDelegate = apiDelegate
        this.methodName = methodName
        this.parameterTypes = parameterTypes
        Class[] metaMethodParams = ([persistentClass] + (parameterTypes as List)) as Class[]
        this.metaMethod = pickMetaMethod(apiDelegate.getMetaClass(), methodName, metaMethodParams, false)
    }

    protected static MetaMethod pickMetaMethod(final MetaClass mc, final String methodName, final Class[] parameterTypes, boolean staticScope) {
        try {
            return mc.pickMethod(methodName, parameterTypes)
        } catch (MethodSelectionException mse) {
            // the metamethod already exists with multiple signatures, pick the most specific
            return mc.methods.find { MetaMethod existingMethod ->
                existingMethod.name == methodName && existingMethod.isStatic()==staticScope && ((!parameterTypes && !existingMethod.parameterTypes) || parameterTypes==existingMethod.parameterTypes)
            }
        }
    }
    
    @Override
    Object call(Object[] args) {
        def delegateArg = Collections.singletonList(delegate).toArray()
        def arguments = args ? ArrayUtils.addAll(delegateArg, args) : delegateArg
        metaMethod.invoke(apiDelegate, arguments)
    }

    Object doCall(Object[] args) {
        call(args)
    }

    @Override
    Class[] getParameterTypes() { parameterTypes }

    @Override
    int getMaximumNumberOfParameters() {
        parameterTypes.length
    }
}