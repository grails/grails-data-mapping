/* 
 * Copyright 2013 the original author or authors.
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
package org.codehaus.groovy.grails.orm.hibernate

import groovy.transform.CompileStatic

import org.grails.datastore.gorm.GormValidationApi
import groovy.transform.CompileStatic

import org.codehaus.groovy.grails.commons.metaclass.DynamicMethodInvocation
import org.grails.datastore.gorm.GormValidationApi

@CompileStatic
abstract class AbstractHibernateGormValidationApi<D> extends GormValidationApi<D> {

    protected ClassLoader classLoader

    protected AbstractHibernateGormValidationApi(Class<D> persistentClass, AbstractHibernateDatastore datastore, ClassLoader classLoader) {
        super(persistentClass, datastore)
        this.classLoader = classLoader
    }

    protected abstract DynamicMethodInvocation getValidateMethod()

    @Override
    boolean validate(D instance) {
        if (getValidateMethod()) {
            return getValidateMethod().invoke(instance, "validate", [] as Object[])
        }
        return super.validate(instance)
    }

    @Override
    boolean validate(D instance, boolean evict) {
        if (getValidateMethod()) {
            return getValidateMethod().invoke(instance, "validate", [evict] as Object[])
        }
        return super.validate(instance, evict)
    }

    @Override
    boolean validate(D instance, Map arguments) {
        if (getValidateMethod()) {
            return getValidateMethod().invoke(instance, "validate", [arguments] as Object[])
        }
        return super.validate(instance, arguments)
    }

    @Override
    boolean validate(D instance, List fields) {
        if (getValidateMethod()) {
            return getValidateMethod().invoke(instance, "validate", [fields] as Object[])
        }
        return super.validate(instance, fields)
    }
}
