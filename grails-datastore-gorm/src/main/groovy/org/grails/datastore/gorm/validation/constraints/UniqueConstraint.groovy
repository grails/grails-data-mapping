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

package org.grails.datastore.gorm.validation.constraints

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.validation.AbstractConstraint
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.engine.EntityPersister
import org.grails.datastore.mapping.reflect.NameUtils
import org.springframework.validation.Errors

/**
 * Implementation of the unique constraint for the datastore abstraction.
 *
 * Note: Uses the deprecated Grails 2.x APIs to maintain compatibility, change to 3.x APIs in the future
 *
 * @author Graeme Rocher
 *
 */
@CompileStatic
class UniqueConstraint extends AbstractConstraint {

    Datastore datastore

    UniqueConstraint(Datastore datastore) {
        this.datastore = datastore
    }

    @Override
    protected void processValidate(Object target, Object propertyValue, Errors errors) {
        withManualFlushMode { Session session ->

            EntityPersister persister = (EntityPersister)session.getPersister(target)
            def id = getIdentifier(target, persister)
            if (constraintParameter instanceof Boolean) {
                if (constraintParameter) {
                    if (propertyValue != null) {

                        final existing = findExistingSimple(propertyValue)
                        if (existing != null) {
                            def existingId = getIdentifier(existing, persister)
                            if (id != existingId) {
                                def args = [ constraintPropertyName, constraintOwningClass, propertyValue ] as Object[]
                                rejectValue(target, errors, "unique", args, getDefaultMessage("default.not.unique.message"))
                            }
                        }
                    }
                }
            }
            else {
                List group = []
                if (constraintParameter instanceof Collection) {
                    group.addAll(constraintParameter)
                }
                else if (constraintParameter != null) {
                    group.add(constraintParameter.toString())
                }

                def notIncludeNull = isNotIncludeNulls(group, target)
                if (notIncludeNull) {
                    def existing = findExisting(group, propertyValue, target)
                    if (existing) {
                        def existingId = getIdentifier(existing, persister)
                        if (id != existingId) {
                            def args = [constraintPropertyName, constraintOwningClass, propertyValue] as Object[]
                            rejectValue(target, errors, "unique", args, getDefaultMessage("default.not.unique.message"))
                        }
                    }
                }
            }
        }
    }

    @CompileDynamic
    private Object findExistingSimple(Object propertyValue) {
        constraintOwningClass."findBy${NameUtils.capitalize(constraintPropertyName)}"(propertyValue)
    }

    @CompileDynamic
    private boolean isNotIncludeNulls(List group, target) {
        group.every { target[it] != null }
    }

    @CompileDynamic
    private Object findExisting(List group, propertyValue, target) {
        def existing = constraintOwningClass.createCriteria().get {
            eq constraintPropertyName, propertyValue
            for (prop in group) {
                eq prop, target[prop]
            }
        }
        return existing
    }

    private Serializable getIdentifier(target, EntityPersister persister) {
        if (target == null) {
            return
        }

        if (persister == null) {
            def entity = datastore.mappingContext.getPersistentEntity(target.class.name)
            if (entity != null) {
                return (Serializable)target[entity.identity.name]
            }
        }
        else {
            return persister.getObjectIdentifier(target)
        }
    }

    @CompileDynamic
    def withManualFlushMode(Closure callable) {
        constraintOwningClass.withSession { Session session ->
            final flushMode = session.getFlushMode()

            try {
                session.setFlushMode(javax.persistence.FlushModeType.COMMIT)
                callable.call(session)
            } finally {
                session.setFlushMode(flushMode)
            }
        }
    }

    /**
     * Return whether the constraint is valid for the owning class
     *
     * @return True if it is
     */
    boolean isValid() {
        final entity = datastore?.getMappingContext()?.getPersistentEntity(constraintOwningClass.name)
        if (entity) {
            return !entity.isExternal()
        }
        return false
    }

    @Override
    boolean supports(Class aClass) { true }

    @Override
    String getName() { "unique" }
}
