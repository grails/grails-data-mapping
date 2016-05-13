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
package org.grails.orm.hibernate

import grails.orm.HibernateCriteriaBuilder
import grails.orm.PagedResultList
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.grails.datastore.gorm.GormEnhancer
import org.grails.orm.hibernate.query.GrailsHibernateQueryUtils
import org.grails.datastore.gorm.finders.DynamicFinder
import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.mapping.query.api.BuildableCriteria as GrailsCriteria
import org.hibernate.Criteria
import org.hibernate.LockMode
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.springframework.core.convert.ConversionService
import org.springframework.orm.hibernate4.SessionFactoryUtils
import org.springframework.orm.hibernate4.SessionHolder
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * The implementation of the GORM static method contract for Hibernate
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
class HibernateGormStaticApi<D> extends AbstractHibernateGormStaticApi<D> {
    protected GrailsHibernateTemplate hibernateTemplate
    protected SessionFactory sessionFactory
    protected ConversionService conversionService
    protected Class identityType
    protected ClassLoader classLoader
    private HibernateGormInstanceApi<D> instanceApi
    private int defaultFlushMode

    HibernateGormStaticApi(Class<D> persistentClass, HibernateDatastore datastore, List<FinderMethod> finders,
                ClassLoader classLoader, PlatformTransactionManager transactionManager) {
        super(persistentClass, datastore, finders, transactionManager, null)
        this.classLoader = classLoader
        sessionFactory = datastore.getSessionFactory()
        conversionService = datastore.mappingContext.conversionService

        identityType = persistentEntity.identity?.type
        hibernateTemplate = new GrailsHibernateTemplate(sessionFactory, datastore)
        this.defaultFlushMode = datastore.getDefaultFlushMode()
        super.hibernateTemplate = hibernateTemplate
        
        instanceApi = new HibernateGormInstanceApi<>(persistentClass, datastore, classLoader)
    }

    @Override
    def propertyMissing(String name) {
        return GormEnhancer.findStaticApi(persistentClass, name)
    }

    @Override
    List<D> list(Map params = Collections.emptyMap()) {
        hibernateTemplate.execute { Session session ->
            Criteria c = session.createCriteria(persistentEntity.javaClass)
            hibernateTemplate.applySettings c

            params = params ? new HashMap(params) : Collections.emptyMap()
            setResultTransformer(c)
            if(params.containsKey(DynamicFinder.ARGUMENT_MAX)) {

                c.setMaxResults(Integer.MAX_VALUE)
                GrailsHibernateQueryUtils.populateArgumentsForCriteria(persistentEntity, c, params, datastore.mappingContext.conversionService, true)
                return new PagedResultList(hibernateTemplate, c)
            }
            else {
                GrailsHibernateQueryUtils.populateArgumentsForCriteria(persistentEntity, c, params, datastore.mappingContext.conversionService, true)
                return c.list()
            }
        }
    }

    @CompileDynamic
    protected void setResultTransformer(Criteria c) {
        c.resultTransformer = Criteria.DISTINCT_ROOT_ENTITY
    }

    @Override
    GrailsCriteria createCriteria() {
        def builder = new HibernateCriteriaBuilder(persistentClass, sessionFactory)
        builder.datastore = (AbstractHibernateDatastore)datastore
        builder.conversionService = conversionService
        builder
    }

    @Override
    D lock(Serializable id) {
        (D)hibernateTemplate.lock((Class)persistentClass, convertIdentifier(id), LockMode.PESSIMISTIC_WRITE)
    }

    @Override
    Integer executeUpdate(String query, Map params, Map args) {
        def template = hibernateTemplate
        SessionFactory sessionFactory = this.sessionFactory
        return (Integer) template.execute { Session session ->
            def q = session.createQuery(query)
            template.applySettings(q)
            def sessionHolder = (SessionHolder) TransactionSynchronizationManager.getResource( sessionFactory )
            if (sessionHolder && sessionHolder.hasTimeout()) {
                q.timeout = sessionHolder.timeToLiveInSeconds
            }

            populateQueryArguments(q, params)
            populateQueryArguments(q, args)
            populateQueryWithNamedArguments(q, params)

            return q.executeUpdate()
        }
    }

    /**
     * @deprecated positional parameters are deprecated
     */
    @Override
    @Deprecated
    Integer executeUpdate(String query, Collection params, Map args) {
        def template = hibernateTemplate
        SessionFactory sessionFactory = this.sessionFactory

        return (Integer) template.execute { Session session ->
            def q = session.createQuery(query)
            template.applySettings(q)
            def sessionHolder = (SessionHolder) TransactionSynchronizationManager.getResource( sessionFactory )
            if (sessionHolder && sessionHolder.hasTimeout()) {
                q.timeout = sessionHolder.timeToLiveInSeconds
            }


            params.eachWithIndex { val, int i ->
                if (val instanceof CharSequence) {
                    q.setParameter i, val.toString()
                }
                else {
                    q.setParameter i, val
                }
            }
            populateQueryArguments(q, args)
            return q.executeUpdate()
        }
    }

    @Override
    Object withSession(Closure callable) {
        GrailsHibernateTemplate template = new GrailsHibernateTemplate(sessionFactory, (HibernateDatastore)datastore)
        template.setExposeNativeSession(false)
        template.setApplyFlushModeOnlyToNonExistingTransactions(true)
        hibernateTemplate.execute new GrailsHibernateTemplate.HibernateCallback() {
            def doInHibernate(Session session) {
                callable(session)
            }
        }
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    def withNewSession(Closure callable) {
        GrailsHibernateTemplate template  = new GrailsHibernateTemplate(sessionFactory, (HibernateDatastore)datastore)
        template.setExposeNativeSession(false)
        SessionHolder sessionHolder = (SessionHolder)TransactionSynchronizationManager.getResource(sessionFactory)
        Session previousSession = sessionHolder?.session
        Session newSession
        boolean newBind = false
        try {
            template.allowCreate = true
            newSession = sessionFactory.openSession()
            if (sessionHolder == null) {
                sessionHolder = new SessionHolder(newSession)
                TransactionSynchronizationManager.bindResource(sessionFactory, sessionHolder)
                newBind = true
            }
            else {
                sessionHolder.@session = newSession
            }

            hibernateTemplate.execute new GrailsHibernateTemplate.HibernateCallback() {
                def doInHibernate(Session session) {
                    callable(session)
                }
            }
        }
        finally {
            try {
                if (newSession) {
                    SessionFactoryUtils.closeSession(newSession)
                }
                if (newBind) {
                    TransactionSynchronizationManager.unbindResource(sessionFactory)
                }
            }
            finally {
                if (previousSession) {
                    sessionHolder.@session = previousSession
                }
            }
        }
    }


}
