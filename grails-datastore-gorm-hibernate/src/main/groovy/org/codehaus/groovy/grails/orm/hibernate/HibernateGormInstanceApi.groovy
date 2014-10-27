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

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil
import org.codehaus.groovy.grails.orm.hibernate.metaclass.MergePersistentMethod
import org.codehaus.groovy.grails.orm.hibernate.metaclass.SavePersistentMethod
import org.hibernate.LockMode
import org.hibernate.engine.EntityEntry
import org.hibernate.engine.SessionImplementor
import org.hibernate.proxy.HibernateProxy
import org.springframework.dao.DataAccessException

/**
 * The implementation of the GORM instance API contract for Hibernate.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
class HibernateGormInstanceApi<D> extends AbstractHibernateGormInstanceApi<D> {

    protected SavePersistentMethod saveMethod
    protected MergePersistentMethod mergeMethod
    protected GrailsHibernateTemplate hibernateTemplate
    protected InstanceApiHelper instanceApiHelper

    HibernateGormInstanceApi(Class<D> persistentClass, HibernateDatastore datastore, ClassLoader classLoader) {
        super(persistentClass, datastore, classLoader)

        def mappingContext = datastore.mappingContext
        def grailsApplication = datastore.getGrailsApplication()
        if (grailsApplication) {
            GrailsDomainClass domainClass = (GrailsDomainClass)grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, persistentClass.name)
            config = (Map)grailsApplication.getFlatConfig().get('grails.gorm')
            saveMethod = new SavePersistentMethod(sessionFactory, classLoader, grailsApplication, domainClass, datastore)
            mergeMethod = new MergePersistentMethod(sessionFactory, classLoader, grailsApplication, domainClass, datastore)
            hibernateTemplate = new GrailsHibernateTemplate(sessionFactory, grailsApplication, datastore.getDefaultFlushMode())
            cacheQueriesByDefault = GrailsHibernateUtil.isCacheQueriesByDefault(grailsApplication)
        }
        else {
            hibernateTemplate = new GrailsHibernateTemplate(sessionFactory)
            hibernateTemplate.setFlushMode(datastore.getDefaultFlushMode())
        }
        instanceApiHelper = new InstanceApiHelper(hibernateTemplate)
    }

    /**
     * Checks whether a field is dirty
     *
     * @param instance The instance
     * @param fieldName The name of the field
     *
     * @return true if the field is dirty
     */
    boolean isDirty(D instance, String fieldName) {
        SessionImplementor session = (SessionImplementor)sessionFactory.currentSession
        def entry = findEntityEntry(instance, session)
        if (!entry || !entry.loadedState) {
            return false
        }

        Object[] values = entry.persister.getPropertyValues(instance, session.entityMode)
        int[] dirtyProperties = entry.persister.findDirty(values, entry.loadedState, instance, session)
        if(!dirtyProperties) return false
        int fieldIndex = entry.persister.propertyNames.findIndexOf { fieldName == it }
        return fieldIndex in dirtyProperties
    }

    /**
     * Checks whether an entity is dirty
     *
     * @param instance The instance
     * @return true if it is dirty
     */
    boolean isDirty(D instance) {
        SessionImplementor session = (SessionImplementor)sessionFactory.currentSession
        def entry = findEntityEntry(instance, session)
        if (!entry || !entry.loadedState) {
            return false
        }

        Object[] values = entry.persister.getPropertyValues(instance, session.entityMode)
        def dirtyProperties = entry.persister.findDirty(values, entry.loadedState, instance, session)
        return dirtyProperties != null
    }

    /**
     * Obtains a list of property names that are dirty
     *
     * @param instance The instance
     * @return A list of property names that are dirty
     */
    List getDirtyPropertyNames(D instance) {
        SessionImplementor session = (SessionImplementor)sessionFactory.currentSession
        def entry = findEntityEntry(instance, session)
        if (!entry || !entry.loadedState) {
            return []
        }

        Object[] values = entry.persister.getPropertyValues(instance, session.entityMode)
        int[] dirtyProperties = entry.persister.findDirty(values, entry.loadedState, instance, session)
        def names = []
        for (index in dirtyProperties) {
            names << entry.persister.propertyNames[index]
        }
        names
    }

    /**
     * Gets the original persisted value of a field.
     *
     * @param fieldName The field name
     * @return The original persisted value
     */
    Object getPersistentValue(D instance, String fieldName) {
        SessionImplementor session = (SessionImplementor)sessionFactory.currentSession
        def entry = findEntityEntry(instance, session, false)
        if (!entry || !entry.loadedState) {
            return null
        }

        int fieldIndex = entry.persister.propertyNames.findIndexOf { fieldName == it }
        return fieldIndex == -1 ? null : entry.loadedState[fieldIndex]
    }

    @Override
    D lock(D instance) {
        hibernateTemplate.lock(instance, LockMode.UPGRADE)
    }

    @Override
    D refresh(D instance) {
        hibernateTemplate.refresh(instance)
        return instance
    }

    @Override
    D save(D instance) {
        if (saveMethod) {
            return (D)saveMethod.invoke(instance, "save", EMPTY_ARRAY)
        }
        else {
            return super.save(instance)
        }
    }

    D save(D instance, boolean validate) {
        if (saveMethod) {
            return (D)saveMethod.invoke(instance, "save", [validate] as Object[])
        }
        else {
            return super.save(instance, validate)
        }
    }

    @Override
    D merge(D instance) {
        if (mergeMethod) {
            return (D)mergeMethod.invoke(instance, "merge", EMPTY_ARRAY)
        }
        else {
            return super.merge(instance)
        }
    }

    @Override
    D merge(D instance, Map params) {
        if (mergeMethod) {
            return (D)mergeMethod.invoke(instance, "merge", [params] as Object[])
        }
        else {
            return super.merge(instance, params)
        }
    }

    @Override
    D save(D instance, Map params) {
        if (saveMethod) {
            return (D)saveMethod.invoke(instance, "save", [params] as Object[])
        }
        return super.save(instance, params)
    }

    @Override
    D attach(D instance) {
        hibernateTemplate.lock(instance, LockMode.NONE)
        return instance
    }

    @Override
    void discard(D instance) {
        hibernateTemplate.evict instance
    }

    @Override
    void delete(D instance) {
        boolean flush = shouldFlush()
        try {
            instanceApiHelper.delete instance, flush
        }
        catch (DataAccessException e) {
            handleDataAccessException(hibernateTemplate, e)
        }
    }

    @Override
    void delete(D instance, Map params) {
        boolean flush = shouldFlush(params)
        try {
            instanceApiHelper.delete instance, flush
        }
        catch (DataAccessException e) {
            handleDataAccessException(hibernateTemplate, e)
        }
    }

    @Override
    boolean instanceOf(D instance, Class cls) {
        if (instance instanceof HibernateProxy) {
            return GrailsHibernateUtil.unwrapProxy(instance) in cls
        }
        return instance in cls
    }

    @Override
    boolean isAttached(D instance) {
        hibernateTemplate.contains instance
    }

    protected EntityEntry findEntityEntry(D instance, SessionImplementor session, boolean forDirtyCheck = true) {
        def entry = session.persistenceContext.getEntry(instance)
        if (!entry) {
            return null
        }

        if (forDirtyCheck && !entry.requiresDirtyCheck(instance) && entry.loadedState) {
            return null
        }

        entry
    }

    /**
     * Session should no longer be flushed after a data access exception occurs (such a constriant violation)
     */
    protected void handleDataAccessException(GrailsHibernateTemplate template, DataAccessException e) {
        try {
            instanceApiHelper.setFlushModeManual()
        }
        finally {
            throw e
        }
    }

    protected boolean shouldFlush(Map map = [:]) {
        if (map?.containsKey('flush')) {
            return Boolean.TRUE == map.flush
        }
        return config?.autoFlush instanceof Boolean ? config.autoFlush : false
    }
}
