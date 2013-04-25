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

import grails.orm.HibernateCriteriaBuilder

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.domain.GrailsDomainClassMappingContext
import org.codehaus.groovy.grails.orm.hibernate.cfg.CompositeIdentity
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsDomainBinder
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil
import org.codehaus.groovy.grails.orm.hibernate.metaclass.ExecuteQueryPersistentMethod
import org.codehaus.groovy.grails.orm.hibernate.metaclass.ExecuteUpdatePersistentMethod
import org.codehaus.groovy.grails.orm.hibernate.metaclass.FindAllPersistentMethod
import org.codehaus.groovy.grails.orm.hibernate.metaclass.FindPersistentMethod
import org.codehaus.groovy.grails.orm.hibernate.metaclass.ListPersistentMethod
import org.codehaus.groovy.grails.orm.hibernate.metaclass.MergePersistentMethod
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.query.api.Criteria as GrailsCriteria
import org.hibernate.Criteria
import org.hibernate.LockMode
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.criterion.Projections
import org.hibernate.criterion.Restrictions
import org.springframework.beans.SimpleTypeConverter
import org.springframework.beans.TypeConverter
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
class HibernateGormStaticApi<D> extends GormStaticApi<D> {
    private static final EMPTY_ARRAY = [] as Object[]

    private GrailsHibernateTemplate hibernateTemplate
    private SessionFactory sessionFactory
    private ConversionService conversionService
    private Class identityType
    private ListPersistentMethod listMethod
    private FindAllPersistentMethod findAllMethod
    private FindPersistentMethod findMethod
    private ExecuteQueryPersistentMethod executeQueryMethod
    private ExecuteUpdatePersistentMethod executeUpdateMethod
    private MergePersistentMethod mergeMethod
    private ClassLoader classLoader
    private GrailsApplication grailsApplication
    private boolean cacheQueriesByDefault = false
    private TypeConverter typeConverter = new SimpleTypeConverter()

    HibernateGormStaticApi(Class persistentClass, HibernateDatastore datastore, List<FinderMethod> finders,
                ClassLoader classLoader, PlatformTransactionManager transactionManager) {
        super(persistentClass, datastore, finders)

        super.transactionManager = transactionManager
        this.classLoader = classLoader
        sessionFactory = datastore.getSessionFactory()
        conversionService = datastore.mappingContext.conversionService

        identityType = persistentEntity.identity?.type

        def mappingContext = datastore.mappingContext
        if (mappingContext instanceof GrailsDomainClassMappingContext) {
            GrailsDomainClassMappingContext domainClassMappingContext = mappingContext
            grailsApplication = domainClassMappingContext.getGrailsApplication()

            GrailsDomainClass domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, persistentClass.name)
            identityType = domainClass.identifier?.type

            mergeMethod = new MergePersistentMethod(sessionFactory, classLoader, grailsApplication, domainClass, datastore)
            listMethod = new ListPersistentMethod(grailsApplication, sessionFactory, classLoader)
            hibernateTemplate = new GrailsHibernateTemplate(sessionFactory, grailsApplication)
            hibernateTemplate.setCacheQueries(cacheQueriesByDefault)
        } else {
            hibernateTemplate = new GrailsHibernateTemplate(sessionFactory)
        }

        executeQueryMethod = new ExecuteQueryPersistentMethod(sessionFactory, classLoader, grailsApplication)
        executeUpdateMethod = new ExecuteUpdatePersistentMethod(sessionFactory, classLoader, grailsApplication)
        findMethod = new FindPersistentMethod(sessionFactory, classLoader, grailsApplication)
        findAllMethod = new FindAllPersistentMethod(sessionFactory, classLoader, grailsApplication)
    }

    @Override
    D get(Serializable id) {
        if (id || (id instanceof Number)) {
            id = convertIdentifier(id)
            final result = hibernateTemplate.get(persistentClass, id)
            return GrailsHibernateUtil.unwrapIfProxy(result)
        }
    }

    private convertIdentifier(Serializable id) {
        if (id instanceof CharSequence) {
            id = id.toString()
        }
        final idType = identityType
        if (idType != null && id != null && !idType.isAssignableFrom(id.getClass())) {
            try {
                if (id instanceof Number && Long.equals(idType)) {
                    id = id.toLong()
                }
                else {
                    id = typeConverter.convertIfNecessary(id, idType)
                }
            } catch (e) {
                // ignore
            }
        }

        return id
    }

    @Override
    D read(Serializable id) {
        if (id == null) {
            return null
        }

        hibernateTemplate.execute new GrailsHibernateTemplate.HibernateCallback() {
            def doInHibernate(Session session) {
               def o = get(id)
               if (o && session.contains(o)) {
                   session.setReadOnly(o, true)
               }
               return o
            }
        }
    }

    @Override
    D load(Serializable id) {
        id = convertIdentifier(id)
        if (id != null) {
            return hibernateTemplate.load(persistentClass, id)
        }
    }

    @Override
    List<D> getAll() {
        hibernateTemplate.execute new GrailsHibernateTemplate.HibernateCallback() {
            def doInHibernate(Session session) {
                Criteria criteria = session.createCriteria(persistentClass)
                hibernateTemplate.applySettings criteria
                criteria.list()
            }
        }
    }

    List<D> getAll(List ids) {
        getAllInternal(ids)
    }

    List<D> getAll(Long... ids) {
        getAllInternal(ids)
    }

    @Override
    List<D> getAll(Serializable... ids) {
        getAllInternal(ids)
    }

    private List getAllInternal(ids) {
        if (!ids) return []

        hibernateTemplate.execute new GrailsHibernateTemplate.HibernateCallback() {
            def doInHibernate(Session session) {
                ids = ids.collect { convertIdentifier(it) }
                def criteria = session.createCriteria(persistentClass)
                hibernateTemplate.applySettings(criteria)
                def identityName = persistentEntity.identity.name
                criteria.add(Restrictions.'in'(identityName, ids))
                def results = criteria.list()
                def idsMap = [:]
                for (object in results) {
                    idsMap[object[identityName]] = object
                }
                results.clear()
                for (id in ids) {
                    results << idsMap[id]
                }
                results
            }
        }
    }

    @Override
    GrailsCriteria createCriteria() {
        def builder = new HibernateCriteriaBuilder(persistentClass, sessionFactory)
        builder.grailsApplication = grailsApplication
        builder
    }

    @Override
    D lock(Serializable id) {
        id = convertIdentifier(id)
        hibernateTemplate.lock(persistentClass, id, LockMode.PESSIMISTIC_WRITE)
    }

    @Override
    D merge(o) {
        mergeMethod.invoke(o, "merge", [] as Object[])
    }

    @Override
    Integer count() {
        hibernateTemplate.execute new GrailsHibernateTemplate.HibernateCallback() {
            def doInHibernate(Session session) {
                def criteria = session.createCriteria(persistentClass)
                hibernateTemplate.applySettings(criteria)
                criteria.setProjection(Projections.rowCount())
                def num = criteria.uniqueResult()
                num == null ? 0 : num
            }
        }
    }

    @Override
    boolean exists(Serializable id) {
        id = convertIdentifier(id)
        hibernateTemplate.execute new GrailsHibernateTemplate.HibernateCallback() {
            def doInHibernate(Session session) {
                Criteria criteria = session.createCriteria(persistentEntity.javaClass)
                hibernateTemplate.applySettings(criteria)
                criteria.add(Restrictions.idEq(id))
                    .setProjection(Projections.rowCount())
                    .uniqueResult()
            }
        }
    }

    @Override
    List<D> list(Map params) {
        listMethod.invoke persistentClass, "list", [params] as Object[]
    }

    @Override
    List<D> list() {
        listMethod.invoke persistentClass, "list", EMPTY_ARRAY
    }

    @Override
    List<D> findAll(example, Map args) {
        findAllMethod.invoke(persistentClass, "findAll", [example, args] as Object[])
    }

    @Override
    D find(example, Map args) {
        findMethod.invoke(persistentClass, "find", [example, args] as Object[])
    }

    D first(Map m) {
        def entityMapping = GrailsDomainBinder.getMapping(persistentEntity.javaClass)
        if (entityMapping?.identity instanceof CompositeIdentity) {
            throw new UnsupportedOperationException('The first() method is not supported for domain classes that have composite keys.')
        }
        super.first(m)
    }

    D last(Map m) {
        def entityMapping = GrailsDomainBinder.getMapping(persistentEntity.javaClass)
        if (entityMapping?.identity instanceof CompositeIdentity) {
            throw new UnsupportedOperationException('The last() method is not supported for domain classes that have composite keys.')
        }
        super.last(m)
    }

    /**
     * Finds a single result for the given query and arguments and a maximum results to return value
     *
     * @param query The query
     * @param args The arguments
     * @param max The maximum to return
     * @return A single result or null
     *
     * @deprecated Use Book.find('..', [foo:'bar], [max:10]) instead
     */
    @Deprecated
    D find(String query, Map args, Integer max) {
        findMethod.invoke(persistentClass, "find", [query, args, max] as Object[])
    }

    /**
     * Finds a single result for the given query and arguments and a maximum results to return value
     *
     * @param query The query
     * @param args The arguments
     * @param max The maximum to return
     * @param offset The offset
     * @return A single result or null
     *
     * @deprecated Use Book.find('..', [foo:'bar], [max:10, offset:5]) instead
     */
    @Deprecated
    D find(String query, Map args, Integer max, Integer offset) {
        findMethod.invoke(persistentClass, "find", [query, args, max, offset] as Object[])
    }

    /**
     * Finds a single result for the given query and a maximum results to return value
     *
     * @param query The query
     * @param max The maximum to return
     * @return A single result or null
     *
     * @deprecated Use Book.find('..', [max:10]) instead
     */
    @Deprecated
    D find(String query, Integer max) {
        findMethod.invoke(persistentClass, "find", [query, max] as Object[])
    }

    /**
     * Finds a single result for the given query and a maximum results to return value
     *
     * @param query The query
     * @param max The maximum to return
     * @param offset The offset to use
     * @return A single result or null
     *
     * @deprecated Use Book.find('..', [max:10, offset:5]) instead
     */
    @Deprecated
    D find(String query, Integer max, Integer offset) {
        findMethod.invoke(persistentClass, "find", [query, max, offset] as Object[])
    }

    /**
     * Finds a list of results for the given query and arguments and a maximum results to return value
     *
     * @param query The query
     * @param args The arguments
     * @param max The maximum to return
     * @return A list of results
     *
     * @deprecated Use findAll('..', [foo:'bar], [max:10]) instead
     */
    @Deprecated
    List<D> findAll(String query, Map args, Integer max) {
        findAllMethod.invoke(persistentClass, "findAll", [query, args, max] as Object[])
    }

    /**
     * Finds a list of results for the given query and arguments and a maximum results to return value
     *
     * @param query The query
     * @param args The arguments
     * @param max The maximum to return
     * @param offset The offset
     *
     * @return A list of results
     *
     * @deprecated Use findAll('..', [foo:'bar], [max:10, offset:5]) instead
     */
    @Deprecated
    List<D> findAll(String query, Map args, Integer max, Integer offset) {
        findAllMethod.invoke(persistentClass, "findAll", [query, args, max, offset] as Object[])
    }

    /**
     * Finds a list of results for the given query and a maximum results to return value
     *
     * @param query The query
     * @param max The maximum to return
     * @return A list of results
     *
     * @deprecated Use findAll('..', [max:10]) instead
     */
    @Deprecated
    List<D> findAll(String query, Integer max) {
        findAllMethod.invoke(persistentClass, "findAll", [query, max] as Object[])
    }

    /**
     * Finds a list of results for the given query and a maximum results to return value
     *
     * @param query The query
     * @param max The maximum to return
     * @return A list of results
     *
     * @deprecated Use findAll('..', [max:10, offset:5]) instead
     */
    @Deprecated
    List<D> findAll(String query, Integer max, Integer offset) {
        findAllMethod.invoke(persistentClass, "findAll", [query, max, offset] as Object[])
    }

    @Override
    List<D> findAllWhere(Map queryMap, Map args) {
        if (!queryMap) return null

        hibernateTemplate.execute new GrailsHibernateTemplate.HibernateCallback() {
            def doInHibernate(Session session) {
                Map queryArgs = filterQueryArgumentMap(queryMap)
                List<String> nullNames = removeNullNames(queryArgs)
                Criteria criteria = session.createCriteria(persistentClass)
                hibernateTemplate.applySettings(criteria)
                criteria.add(Restrictions.allEq(queryArgs))
                for (name in nullNames) {
                    criteria.add Restrictions.isNull(name)
                }
                criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
                criteria.list()
            }
        }
    }

    @Override
    D findWhere(Map queryMap, Map args) {
        if (!queryMap) return null

        hibernateTemplate.execute new GrailsHibernateTemplate.HibernateCallback() {
            def doInHibernate(Session session) {
                Map queryArgs = filterQueryArgumentMap(queryMap)
                List<String> nullNames = removeNullNames(queryArgs)
                Criteria criteria = session.createCriteria(persistentClass)
                hibernateTemplate.applySettings(criteria)
                criteria.add(Restrictions.allEq(queryArgs))
                for (name in nullNames) {
                    criteria.add Restrictions.isNull(name)
                }
                criteria.setMaxResults(1)
                GrailsHibernateUtil.unwrapIfProxy(criteria.uniqueResult())
            }
        }
    }

    private Map filterQueryArgumentMap(Map query) {
        def queryArgs = [:]
        for (entry in query) {
            if (entry.value instanceof CharSequence) {
                queryArgs[entry.key] = entry.value.toString()
            }
            else {
                queryArgs[entry.key] = entry.value
            }
        }
        return queryArgs
    }

    private List<String> removeNullNames(Map query) {
        List<String> nullNames = []
        Set<String> allNames = new HashSet(query.keySet())
        for (String name in allNames) {
            if (query[name] == null) {
                query.remove name
                nullNames << name
            }
        }
        nullNames
    }

    @Override
    Object withSession(Closure callable) {
        GrailsHibernateTemplate template = new GrailsHibernateTemplate(sessionFactory, grailsApplication)
        template.setExposeNativeSession(false)
        hibernateTemplate.execute new GrailsHibernateTemplate.HibernateCallback() {
            def doInHibernate(Session session) {
                callable(session)
            }
        }
    }

    @Override
    def withNewSession(Closure callable) {
        GrailsHibernateTemplate template  = new GrailsHibernateTemplate(sessionFactory, grailsApplication)
        template.setExposeNativeSession(false)
        SessionHolder sessionHolder = TransactionSynchronizationManager.getResource(sessionFactory)
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

    @Override
    List<D> executeQuery(String query) {
        executeQueryMethod.invoke(persistentClass, "executeQuery", [query] as Object[])
    }

    List<D> executeQuery(String query, arg) {
        executeQueryMethod.invoke(persistentClass, "executeQuery", [query, arg] as Object[])
    }

    @Override
    List<D> executeQuery(String query, Map args) {
        executeQueryMethod.invoke(persistentClass, "executeQuery", [query, args] as Object[])
    }

    @Override
    List<D> executeQuery(String query, Map params, Map args) {
        executeQueryMethod.invoke(persistentClass, "executeQuery", [query, params, args] as Object[])
    }

    @Override
    List<D> executeQuery(String query, Collection params) {
        executeQueryMethod.invoke(persistentClass, "executeQuery", [query, params] as Object[])
    }

    @Override
    List<D> executeQuery(String query, Collection params, Map args) {
        executeQueryMethod.invoke(persistentClass, "executeQuery", [query, params, args] as Object[])
    }

    @Override
    Integer executeUpdate(String query) {
        executeUpdateMethod.invoke(persistentClass, "executeUpdate", [query] as Object[])
    }

    @Override
    Integer executeUpdate(String query, Map args) {
        executeUpdateMethod.invoke(persistentClass, "executeUpdate", [query, args] as Object[])
    }

    @Override
    Integer executeUpdate(String query, Map params, Map args) {
        executeUpdateMethod.invoke(persistentClass, "executeUpdate", [query, params, args] as Object[])
    }

    @Override
    Integer executeUpdate(String query, Collection params) {
        executeUpdateMethod.invoke(persistentClass, "executeUpdate", [query, params] as Object[])
    }

    @Override
    Integer executeUpdate(String query, Collection params, Map args) {
        executeUpdateMethod.invoke(persistentClass, "executeUpdate", [query, params, args] as Object[])
    }

    @Override
    D find(String query) {
        findMethod.invoke(persistentClass, "find", [query] as Object[])
    }

    D find(String query, Object[] params) {
        findMethod.invoke(persistentClass, "find", [query, params] as Object[])
    }

    @Override
    D find(String query, Map args) {
        findMethod.invoke(persistentClass, "find", [query, args] as Object[])
    }

    @Override
    D find(String query, Map params, Map args) {
        findMethod.invoke(persistentClass, "find", [query, params, args] as Object[])
    }

    @Override
    Object find(String query, Collection params) {
        findMethod.invoke(persistentClass, "find", [query, params] as Object[])
    }

    @Override
    D find(String query, Collection params, Map args) {
        findMethod.invoke(persistentClass, "find", [query, params, args] as Object[])
    }

    @Override
    List<D> findAll(String query) {
        findAllMethod.invoke(persistentClass, "findAll", [query] as Object[])
    }

    @Override
    List<D> findAll(String query, Map args) {
        findAllMethod.invoke(persistentClass, "findAll", [query, args] as Object[])
    }

    @Override
    List<D> findAll(String query, Map params, Map args) {
        findAllMethod.invoke(persistentClass, "findAll", [query, params, args] as Object[])
    }

    @Override
    List<D> findAll(String query, Collection params) {
        findAllMethod.invoke(persistentClass, "findAll", [query, params] as Object[])
    }

    @Override
    List<D> findAll(String query, Collection params, Map args) {
        findAllMethod.invoke(persistentClass, "findAll", [query, params, args] as Object[])
    }
}
