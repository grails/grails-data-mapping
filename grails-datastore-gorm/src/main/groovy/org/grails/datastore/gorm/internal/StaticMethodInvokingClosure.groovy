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

/**
 * Not public API. Used by GormEnhancer
 */
@SuppressWarnings("rawtypes")
class StaticMethodInvokingClosure extends Closure {

    private String methodName
    private Object apiDelegate
    private Class[] parameterTypes

    StaticMethodInvokingClosure(apiDelegate, String methodName, Class[] parameterTypes) {
        super(apiDelegate, apiDelegate)
        this.apiDelegate = apiDelegate
        this.methodName = methodName
        this.parameterTypes = parameterTypes
    }

    @Override
    Object call(Object[] args) {
        apiDelegate."$methodName"(*args)
    }

    Object doCall(Object[] args) {
        apiDelegate."$methodName"(*args)
    }

    @Override
    Class[] getParameterTypes() { parameterTypes }
}
