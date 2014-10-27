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

import grails.core.GrailsDomainClass
import groovy.transform.CompileStatic

import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil
import org.grails.core.artefact.DomainClassArtefactHandler
import org.hibernate.engine.spi.EntityEntry
import org.hibernate.engine.spi.SessionImplementor

/**
 * The implementation of the GORM instance API contract for Hibernate.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
class HibernateGormInstanceApi<D> extends AbstractHibernateGormInstanceApi<D> {

    protected GrailsHibernateTemplate hibernateTemplate
    protected InstanceApiHelper instanceApiHelper

    HibernateGormInstanceApi(Class<D> persistentClass, HibernateDatastore datastore, ClassLoader classLoader) {
        super(persistentClass, datastore, classLoader, new GrailsHibernateTemplate(datastore.sessionFactory))

        def grailsApplication = datastore.getGrailsApplication()
        if (grailsApplication) {
            GrailsDomainClass domainClass = (GrailsDomainClass)grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, persistentClass.name)
            config = (Map)grailsApplication.getFlatConfig().get('grails.gorm')
            hibernateTemplate = new GrailsHibernateTemplate(sessionFactory, grailsApplication)
            cacheQueriesByDefault = GrailsHibernateUtil.isCacheQueriesByDefault(grailsApplication)
        }
        else {
            hibernateTemplate = new GrailsHibernateTemplate(sessionFactory)
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

        Object[] values = entry.persister.getPropertyValues(instance)
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

        Object[] values = entry.persister.getPropertyValues(instance)
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

        Object[] values = entry.persister.getPropertyValues(instance)
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

    @Override
    void setObjectToReadWrite(Object target) {
        GrailsHibernateUtil.setObjectToReadWrite(target, sessionFactory)
    }
}
