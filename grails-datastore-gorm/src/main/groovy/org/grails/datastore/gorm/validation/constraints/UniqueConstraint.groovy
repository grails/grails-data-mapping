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

import org.codehaus.groovy.grails.validation.AbstractConstraint
import org.springframework.validation.Errors
import org.grails.datastore.mapping.core.Session
import grails.util.GrailsNameUtils
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.engine.EntityPersister

/**
 * Implementation of the unique constraint for the datastore abstraction
 */
class UniqueConstraint extends AbstractConstraint{

    Datastore datastore

    UniqueConstraint(Datastore datastore) {
        this.datastore = datastore
    }

    @Override
    protected void processValidate(Object target, Object propertyValue, Errors errors) {
        withManualFlushMode { Session session ->

            EntityPersister persister = session.getPersister(target)
            def id = persister.getObjectIdentifier(target)
            if(constraintParameter instanceof Boolean) {
                if(constraintParameter) {
                    if(propertyValue != null) {

                        final existing = constraintOwningClass."findBy${GrailsNameUtils.getClassName(constraintPropertyName, '')}"(propertyValue)
                        if(existing != null) {
                            def existingId = persister.getObjectIdentifier(existing)
                            if(id != existingId) {
                                def args = [ constraintPropertyName, constraintOwningClass, propertyValue ] as Object[]
                                rejectValue(target, errors, "unique", args, getDefaultMessage("default.not.unique.message"));
                            }
                        }
                    }
                }
            }
            else {
                List group = []
                if(constraintParameter instanceof Collection) {
                    group.addAll(constraintParameter)
                }
                else if(constraintParameter != null) {
                    group.add(constraintParameter.toString())
                }



                final existing = constraintOwningClass.createCriteria().get {
                    eq constraintPropertyName, propertyValue
                    for(prop in group) {
                        eq prop, target[prop]
                    }
                }

                if(existing) {
                    def existingId = persister.getObjectIdentifier(existing)
                    if(id != existingId) {
                        def args = [ constraintPropertyName, constraintOwningClass, propertyValue ] as Object[]
                        rejectValue(target, errors, "unique", args, getDefaultMessage("default.not.unique.message"));
                    }
                }
            }
        }
    }


    def withManualFlushMode(Closure callable) {
        constraintOwningClass.withSession { Session session ->
            final flushMode = session.getFlushMode()

            try {
                callable.call(session)
                session.setFlushMode(javax.persistence.FlushModeType.COMMIT)
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
    public boolean isValid() {
        final entity = datastore?.getMappingContext()?.getPersistentEntity(constraintOwningClass.name)
        if(entity) {
            return !entity.isExternal()
        }
        return false;
    }

    @Override
    boolean supports(Class aClass) {
        return true
    }

    @Override
    String getName() { "unique" }
}
