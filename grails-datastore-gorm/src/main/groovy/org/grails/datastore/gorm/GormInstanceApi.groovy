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
package org.grails.datastore.gorm

import groovy.transform.CompileStatic

import org.codehaus.groovy.runtime.InvokerHelper
import org.grails.datastore.mapping.dirty.checking.DirtyCheckingSupport
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.core.SessionCallback
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import org.grails.datastore.mapping.proxy.EntityProxy
import org.grails.datastore.mapping.validation.ValidationException
import org.springframework.util.ClassUtils

/**
 * Instance methods of the GORM API.
 *
 * @author Graeme Rocher
 * @param <D> the entity/domain class
 */
@CompileStatic
class GormInstanceApi<D> extends AbstractGormApi<D> {



    protected Class<? extends Exception> validationException = ValidationException
    boolean failOnError = false

    GormInstanceApi(Class<D> persistentClass, Datastore datastore) {
        super(persistentClass, datastore)

        def cl = Thread.currentThread().contextClassLoader
        if(ClassUtils.isPresent("grails.validation.ValidationException", cl)) {
            validationException = (Class<? extends Exception>) ClassUtils.forName("grails.validation.ValidationException", cl)
        }
    }

    /**
     * Proxy aware instanceOf implementation.
     */
    protected boolean instanceOf(D o, Class cls) {
        if (o instanceof EntityProxy) {
            o = (D)((EntityProxy)o).getTarget()
        }
        return o in cls
    }

    /**
     * Upgrades an existing persistence instance to a write lock
     * @return The instance
     */
    protected D lock(D instance) {
        execute({ Session session ->
            session.lock(instance)
            return instance
        } as SessionCallback)
    }

    /**
     * Locks the instance for updates for the scope of the passed closure
     *
     * @param callable The closure
     * @return The result of the closure
     */
    protected def mutex(D instance, Closure callable) {
        execute({ Session session ->
            try {
                session.lock(instance)
                callable?.call()
            }
            finally {
                session.unlock(instance)
            }
        } as SessionCallback)
    }

    /**
     * Refreshes the state of the current instance
     * @param instance The instance
     * @return The instance
     */
    protected D refresh(D instance) {
        execute({ Session session ->
            session.refresh instance
            return instance
        } as SessionCallback)
    }

    /**
     * Saves an object the datastore
     * @param instance The instance
     * @return Returns the instance
     */
    protected D save(D instance) {
        save(instance, Collections.emptyMap())
    }

    /**
     * Forces an insert of an object to the datastore
     * @param instance The instance
     * @return Returns the instance
     */
    protected D insert(D instance) {
        insert(instance, Collections.emptyMap())
    }

    /**
     * Forces an insert of an object to the datastore
     * @param instance The instance
     * @return Returns the instance
     */
    protected D insert(D instance, Map params) {
        execute({ Session session ->
            doSave instance, params, session, true
        } as SessionCallback)
    }

    /**
     * Saves an object the datastore
     * @param instance The instance
     * @return Returns the instance
     */
    protected D merge(D instance) {
        save(instance, Collections.emptyMap())
    }

    /**
     * Saves an object the datastore
     * @param instance The instance
     * @return Returns the instance
     */
    protected D merge(D instance, Map params) {
        save(instance, params)
    }

    /**
     * Save method that takes a boolean which indicates whether to perform validation or not
     *
     * @param instance The instance
     * @param validate Whether to perform validation
     *
     * @return The instance or null if validation fails
     */
    protected D save(D instance, boolean validate) {
        save(instance, [validate: validate])
    }

    /**
     * Saves an object with the given parameters
     * @param instance The instance
     * @param params The parameters
     * @return The instance
     */
    protected D save(D instance, Map params) {
        execute({ Session session ->
            doSave instance, params, session
        } as SessionCallback)
    }

    protected D doSave(D instance, Map params, Session session, boolean isInsert = false) {
        boolean hasErrors = false
        boolean validate = params?.containsKey("validate") ? params.validate : true
        MetaMethod validateMethod = instance.respondsTo('validate', InvokerHelper.EMPTY_ARGS).find { MetaMethod mm -> mm.parameterTypes.length == 0 && !mm.vargsMethod}
        if (validateMethod && validate) {
            session.datastore.setSkipValidation(instance, false)
            hasErrors = !validateMethod.invoke(instance, InvokerHelper.EMPTY_ARGS)
        }
        else {
            session.datastore.setSkipValidation(instance, true)
            InvokerHelper.invokeMethod(instance, "clearErrors", null)
        }

        if (hasErrors) {
            boolean failOnErrorEnabled = params?.containsKey("failOnError") ? params.failOnError : failOnError
            if (failOnErrorEnabled) {
                throw validationException.newInstance("Validation error occurred during call to save()", InvokerHelper.getProperty(instance, "errors"))
            }
            return null
        }
        if (isInsert) {
            session.insert(instance)
        }
        else {
            if(instance instanceof DirtyCheckable) {
                // since this is an explicit call to save() we mark the instance as dirty to ensure it happens
                instance.markDirty()
            }
            session.persist(instance)
        }
        if (params?.flush) {
            session.flush()
        }
        return instance
    }

    /**
     * Returns the objects identifier
     */
    protected Serializable ident(D instance) {
        (Serializable)instance[persistentEntity.getIdentity().name]
    }

    /**
     * Attaches an instance to an existing session. Requries a session-based model
     * @param instance The instance
     * @return
     */
    protected D attach(D instance) {
        execute({ Session session ->
            session.attach(instance)
            instance
        } as SessionCallback)
    }

    /**
     * No concept of session-based model so defaults to true
     */
    protected boolean isAttached(D instance) {
        execute({ Session session ->
            session.contains(instance)
        } as SessionCallback)
    }

    /**
     * Discards any pending changes. Requires a session-based model.
     */
    protected void discard(D instance) {
        execute({ Session session ->
            session.clear(instance)
        } as SessionCallback)
    }

    /**
     * Deletes an instance from the datastore
     * @param instance The instance to delete
     */
    protected void delete(D instance) {
        delete(instance, Collections.emptyMap())
    }

    /**
     * Deletes an instance from the datastore
     * @param instance The instance to delete
     */
    protected void delete(D instance, Map params) {
        execute({ Session session ->
            session.delete(instance)
            if (params?.flush) {
                session.flush()
            }
        } as SessionCallback)
    }

    /**
     * Checks whether a field is dirty
     *
     * @param instance The instance
     * @param fieldName The name of the field
     *
     * @return true if the field is dirty
     */
    protected boolean isDirty(D instance, String fieldName) {
        if(instance instanceof DirtyCheckable) {
            return ((DirtyCheckable)instance).hasChanged(fieldName)
        }
        return true
    }

    /**
     * Checks whether an entity is dirty
     *
     * @param instance The instance
     * @return true if it is dirty
     */
    protected boolean isDirty(D instance) {
        if(instance instanceof DirtyCheckable) {
            return ((DirtyCheckable)instance).hasChanged() || (datastore.hasCurrentSession() && DirtyCheckingSupport.areAssociationsDirty(datastore.currentSession, persistentEntity, instance))
        }
        return true
    }

    /**
     * Obtains a list of property names that are dirty
     *
     * @param instance The instance
     * @return A list of property names that are dirty
     */
    protected List getDirtyPropertyNames(D instance) {
        if(instance instanceof DirtyCheckable) {
            return ((DirtyCheckable)instance).listDirtyPropertyNames()
        }
        return []
    }

    /**
     * Gets the original persisted value of a field.
     *
     * @param fieldName The field name
     * @return The original persisted value
     */
    protected Object getPersistentValue(D instance, String fieldName) {
        if(instance instanceof DirtyCheckable) {
            return ((DirtyCheckable)instance).getOriginalValue(fieldName)
        }
        return null
    }
}
