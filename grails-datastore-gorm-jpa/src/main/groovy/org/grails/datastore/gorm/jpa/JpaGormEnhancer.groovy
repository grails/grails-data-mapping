/* Copyright (C) 2011 SpringSource
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

package org.grails.datastore.gorm.jpa

import static org.grails.datastore.mapping.validation.ValidatingEventListener.*

import javax.persistence.EntityManager
import javax.persistence.Query

import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormInstanceApi
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.core.SessionCallback
import org.grails.datastore.mapping.core.VoidSessionCallback
import org.grails.datastore.mapping.jpa.JpaSession
import org.springframework.core.convert.ConversionService
import org.springframework.orm.jpa.JpaCallback
import org.springframework.orm.jpa.JpaTemplate
import org.springframework.transaction.PlatformTransactionManager

/**
 * Extends the default {@link GormEnhancer} adding supporting for JPQL methods.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class JpaGormEnhancer extends GormEnhancer {

    JpaGormEnhancer(Datastore datastore, PlatformTransactionManager transactionManager) {
        super(datastore, transactionManager)
    }

    JpaGormEnhancer(Datastore datastore) {
        super(datastore)
    }

    protected <D> GormInstanceApi<D> getInstanceApi(Class<D> cls) {
        final api = new JpaInstanceApi<D>(cls, datastore)
        api.failOnError = failOnError
        return api
    }

    protected <D> GormStaticApi<D> getStaticApi(Class<D> cls) {
        return new JpaStaticApi<D>(cls, datastore, finders, transactionManager)
    }
}

class JpaInstanceApi<D> extends GormInstanceApi<D> {

    JpaInstanceApi(Class<D> persistentClass, Datastore datastore) {
        super(persistentClass, datastore)
    }

    @Override
    protected void execute(VoidSessionCallback callback) {
        def session = datastore.connect()
        callback.doInSession(session)
    }

    @Override
    protected def <T> T execute(SessionCallback<T> callback) {
        def session = datastore.connect()
        callback.doInSession(session)
    }

    D merge(instance, Map params) {
        def merged
        doSave(instance, params) { session ->
            merged = session.merge(instance)
        }
        merged
    }

    D save(instance, Map params) {
        doSave(instance, params) { session ->
            session.persist(instance)
        }
    }

    private D doSave(instance, Map params, Closure callable) {
        execute (new SessionCallback() {
            def doInSession(Session session) {
                boolean hasErrors = false
                boolean validate = params?.containsKey("validate") ? params.validate : true
                if (instance.respondsTo('validate') && validate) {
                    session.datastore.setSkipValidation(instance, false)
                    hasErrors = !instance.validate()
                }
                else {
                    session.datastore.setSkipValidation(instance, true)
                    instance.clearErrors()
                }

                if (hasErrors) {
                    if (params?.failOnError) {
                        throw getValidationException().newInstance(
                            "Validation error occured during call to save()", instance.errors)
                    }
                    rollbackTransaction(session)
                    return null
                }

                callable.call(session)
                if (params?.flush) {
                    session.flush()
                }
                return instance
            }
        })
    }

    private void rollbackTransaction(JpaSession jpaSession) {
        jpaSession.getTransaction()?.rollback()
    }
}

class JpaStaticApi<D> extends GormStaticApi<D> {
    JpaStaticApi(Class<D> persistentClass, Datastore datastore, List<FinderMethod> finders) {
        this(persistentClass, datastore, finders, null)        
    }
    
    JpaStaticApi(Class<D> persistentClass, Datastore datastore, List<FinderMethod> finders, PlatformTransactionManager transactionManager) {
        super(persistentClass, datastore, finders, transactionManager)
    }

    def withEntityManager(Closure callable) {
        execute (new SessionCallback() {
            def doInSession(Session session) {
                JpaTemplate jpaTemplate = session.getNativeInterface()
                jpaTemplate.execute({ EntityManager em -> callable.call(em) } as JpaCallback)
            }
        })
    }

    @Override
    protected void execute(VoidSessionCallback callback) {
        def session = datastore.connect()
        callback.doInSession(session)
    }

    @Override
    protected def <T> T execute(SessionCallback<T> callback) {
        def session = datastore.connect()
        callback.doInSession(session)
    }

    @Override
    List<D> executeQuery(String query) {
        doQuery query
    }

    @Override
    List<D> executeQuery(String query, Map args) {
        doQuery query, null, args
    }

    @Override
    List<D> executeQuery(String query, Map params, Map args) {
        doQuery query, params, args, false
    }

    @Override
    List<D> executeQuery(String query, Collection params) {
        doQuery query, params
    }

    @Override
    List<D> executeQuery(String query, Collection params, Map args) {
        doQuery query, params, args
    }

    @Override
    Integer executeUpdate(String query) {
        doUpdate query
    }

    @Override
    Integer executeUpdate(String query, Map args) {
        doUpdate query, null, args
    }

    @Override
    Integer executeUpdate(String query, Map params, Map args) {
        doUpdate query, params, args
    }

    @Override
    Integer executeUpdate(String query, Collection params) {
        doUpdate query, params
    }

    @Override
    Integer executeUpdate(String query, Collection params, Map args) {
        doUpdate query, params, args
    }

    @Override
    D find(String query) {
        doQuery query, null, null, true
    }

    @Override
    D find(String query, Map args) {
        doQuery query, null, args, true
    }

    @Override
    D find(String query, Map params, Map args) {
        doQuery query, params, args, true
    }

    @Override
    D find(String query, Collection params) {
        doQuery query, params, null, true
    }

    @Override
    D find(String query, Collection params, Map args) {
        doQuery query, params, args, true
    }

    @Override
    List<D> findAll(String query) {
        doQuery query
    }

    @Override
    List<D> findAll(String query, Map args) {
        doQuery query, null, args
    }

    @Override
    List<D> findAll(String query, Map params, Map args) {
        doQuery query, params, args
    }

    @Override
    List<D> findAll(String query, Collection params) {
        doQuery query, params
    }

    @Override
    List<D> findAll(String query, Collection params, Map args) {
        doQuery query, params, args
    }

    private Integer doUpdate(String query, params = null, args = null) {
        execute (new SessionCallback<Integer>() {
            Integer doInSession(Session session) {
                JpaTemplate jpaTemplate = session.getNativeInterface()
                jpaTemplate.execute({ EntityManager em ->
                    Query q = em.createQuery(query)
                    populateQueryArguments(datastore, q, params)
                    populateQueryArguments(datastore, q, args)
                    handleParamsAndArguments(q, params, args)

                    q.executeUpdate()
                } as JpaCallback)
            }
        })
    }

    private doQuery(String query, params = null, args = null, boolean singleResult = false) {
        execute (new SessionCallback() {
            def doInSession(Session session) {
                JpaTemplate jpaTemplate = session.getNativeInterface()
                jpaTemplate.execute({ EntityManager em ->
                    Query q = em.createQuery(query)
                    populateQueryArguments(datastore, q, args)
                    populateQueryArguments(datastore, q, params)
                    handleParamsAndArguments(q, params, args)

                    if (singleResult) {
                        doSingleResult(q)
                    }
                    else {
                        return q.resultList
                    }
                } as JpaCallback)
            }
        })
    }

    private Query handleParamsAndArguments(Query q, params, args) {
        if (params || args) {
            if (params instanceof Collection) {
                params.eachWithIndex { val, i ->
                    q.setParameter i+1, val
                }
            }
            else {
                if (params) {
                    for (entry in params) {
                        q.setParameter entry.key, entry.value
                    }
                }
                if (args) {
                    for (entry in args) {
                        q.setParameter entry.key, entry.value
                    }
                }
            }
        }
        return q
    }

    private doSingleResult(Query q) {
        q.setMaxResults 1
        q.resultList[0]
    }

    private void populateQueryArguments(Datastore datastore, Query q, args) {
        if (!(args instanceof Map)) {
            return
        }

        ConversionService conversionService = datastore.mappingContext.conversionService
        if (args?.max) {
            q.setMaxResults(conversionService.convert(args.remove('max'), Integer))
        }
        if (args?.offset) {
            q.setFirstResult(conversionService.convert(args.remove('offset'), Integer))
        }
    }
}
