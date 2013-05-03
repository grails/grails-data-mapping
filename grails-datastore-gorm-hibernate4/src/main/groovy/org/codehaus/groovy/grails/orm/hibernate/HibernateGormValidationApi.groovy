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

import groovy.transform.CompileStatic;

import org.codehaus.groovy.grails.domain.GrailsDomainClassMappingContext
import org.codehaus.groovy.grails.orm.hibernate.metaclass.ValidatePersistentMethod
import org.grails.datastore.gorm.GormValidationApi

@CompileStatic
class HibernateGormValidationApi<D> extends GormValidationApi<D> {

    private ClassLoader classLoader

    ValidatePersistentMethod validateMethod

    HibernateGormValidationApi(Class<D> persistentClass, HibernateDatastore datastore, ClassLoader classLoader) {
        super(persistentClass, datastore)

        this.classLoader = classLoader

        def sessionFactory = datastore.getSessionFactory()

        def mappingContext = datastore.mappingContext
        if (mappingContext instanceof GrailsDomainClassMappingContext) {
            GrailsDomainClassMappingContext domainClassMappingContext = (GrailsDomainClassMappingContext)mappingContext
            def grailsApplication = domainClassMappingContext.getGrailsApplication()
            def validator = mappingContext.getEntityValidator(
                    mappingContext.getPersistentEntity(persistentClass.name))
            validateMethod = new ValidatePersistentMethod(sessionFactory,
                    classLoader, grailsApplication, validator, datastore)
        }
    }

    @Override
    boolean validate(D instance) {
        if (validateMethod) {
            return validateMethod.invoke(instance, "validate", [] as Object[])
        }
        return super.validate(instance)
    }

    @Override
    boolean validate(D instance, boolean evict) {
        if (validateMethod) {
            return validateMethod.invoke(instance, "validate", [evict] as Object[])
        }
        return super.validate(instance, evict)
    }

    @Override
    boolean validate(D instance, Map arguments) {
        if (validateMethod) {
            return validateMethod.invoke(instance, "validate", [arguments] as Object[])
        }
        return super.validate(instance, arguments)
    }

    @Override
    boolean validate(D instance, List fields) {
        if (validateMethod) {
            return validateMethod.invoke(instance, "validate", [fields] as Object[])
        }
        return super.validate(instance, fields)
    }
}
