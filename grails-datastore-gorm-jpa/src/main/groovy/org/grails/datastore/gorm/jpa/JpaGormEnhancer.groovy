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

import static org.springframework.datastore.mapping.validation.ValidatingInterceptor.*

import javax.persistence.EntityManager
import javax.persistence.Query

import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormInstanceApi
import org.grails.datastore.gorm.GormStaticApi
import org.springframework.core.convert.ConversionService
import org.springframework.datastore.mapping.core.Datastore
import org.springframework.datastore.mapping.jpa.JpaSession
import org.springframework.orm.jpa.JpaCallback
import org.springframework.orm.jpa.JpaTemplate
import org.springframework.transaction.PlatformTransactionManager

/**
 * Extends the default {@link GormEnhancer} adding supporting for JPQL methods
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class JpaGormEnhancer extends GormEnhancer{

    JpaGormEnhancer(Datastore datastore, PlatformTransactionManager transactionManager) {
        super(datastore, transactionManager)
    }

    JpaGormEnhancer(Datastore datastore) {
        super(datastore)
    }

    protected GormInstanceApi getInstanceApi(Class cls) {
        return new JpaInstanceApi(cls, datastore)
    }

    protected GormStaticApi getStaticApi(Class cls) {
        return new JpaStaticApi(cls, datastore)
    }
}

class JpaInstanceApi extends GormInstanceApi {

    JpaInstanceApi(Class persistentClass, Datastore datastore) {
        super(persistentClass, datastore)
    }

    @Override
    Object merge(Object instance, Map params) {
        def merged
        doSave(instance, params) { session ->
            merged = session.merge(instance)
        }
        return merged
    }

    @Override
    Object save(Object instance, Map params) {
        doSave(instance, params) { session ->
            session.persist(instance)
        }
    }

    private doSave(instance, Map params, Closure callable) {
        final session = datastore.currentSession
        boolean hasErrors = false
        boolean validate = params?.containsKey("validate") ? params.validate : true
        if (instance.respondsTo('validate') && validate) {
            session.setAttribute(instance, SKIP_VALIDATION_ATTRIBUTE, false)
            hasErrors = !instance.validate()
        }
        else {
            session.setAttribute(instance, SKIP_VALIDATION_ATTRIBUTE, true)
            instance.clearErrors()
        }

        if (!hasErrors) {
            callable.call(session)
            if (params?.flush) {
                session.flush()
            }
        }
        else {
            if (params?.failOnError) {
                throw validationException.newInstance( "Validation error occured during call to save()", instance.errors)
            }
            rollbackTransaction(session)
            return null
        }
        return instance
    }

    private void rollbackTransaction(JpaSession jpaSession) {
        jpaSession.getTransaction()?.rollback()
    }
}

class JpaStaticApi extends GormStaticApi {

    JpaStaticApi(Class persistentClass, Datastore datastore) {
        super(persistentClass, datastore)
    }

    def withEntityManager(Closure callable) {
        JpaTemplate jpaTemplate = datastore.currentSession.getNativeInterface()
        jpaTemplate.execute({ EntityManager em ->
            callable.call( em )
        } as JpaCallback)
    }

    @Override
    Object executeQuery(String query) {
        doQuery query
    }

    @Override
    Object executeQuery(String query, Map args) {
        doQuery query, null, args
    }

    @Override
    Object executeQuery(String query, Map params, Map args) {
        doQuery query, params, args, false
    }

    @Override
    Object executeQuery(String query, Collection params) {
        doQuery query, params
    }

    @Override
    Object executeQuery(String query, Collection params, Map args) {
        doQuery query, params, args
    }

    @Override
    executeUpdate(String query) {
        doUpdate query
    }

    @Override
    executeUpdate(String query, Map args) {
        doUpdate query, null, args
    }

    @Override
    executeUpdate(String query, Map params, Map args) {
        doUpdate query, params, args
    }

    @Override
    executeUpdate(String query, Collection params) {
        doUpdate query, params
    }

    @Override
    executeUpdate(String query, Collection params, Map args) {
        doUpdate query, params, args
    }

    @Override
    Object find(String query) {
        doQuery query, null, null, true
    }

    @Override
    Object find(String query, Map args) {
        doQuery query, null, args, true
    }

    @Override
    Object find(String query, Map params, Map args) {
        doQuery query, params, args, true
    }

    @Override
    Object find(String query, Collection params) {
        doQuery query, params, null, true
    }

    @Override
    Object find(String query, Collection params, Map args) {
        doQuery query, params, args, true
    }

    @Override
    List findAll(String query) {
        doQuery query
    }

    @Override
    List findAll(String query, Map args) {
        doQuery query, null, args
    }

    @Override
    List findAll(String query, Map params, Map args) {
        doQuery query, params, args
    }

    @Override
    List findAll(String query, Collection params) {
        doQuery query, params
    }

    @Override
    List findAll(String query, Collection params, Map args) {
        doQuery query, params, args
    }

    private doUpdate(String query, params = null, args = null) {
        JpaTemplate jpaTemplate = datastore.currentSession.getNativeInterface()
        jpaTemplate.execute({ EntityManager em ->
            Query q = em.createQuery(query)
            populateQueryArguments(datastore, q, params)
            populateQueryArguments(datastore, q, args)
            handleParamsAndArguments(q, params, args)

            q.executeUpdate()
        } as JpaCallback)
    }

    private doQuery(String query, params = null, args = null, boolean singleResult = false) {
        JpaTemplate jpaTemplate = datastore.currentSession.getNativeInterface()
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
        def results = q.resultList

        if (results) {
            results.get 0
        }
    }

    private void populateQueryArguments(Datastore datastore, Query q, args) {
        if (args instanceof Map) {
            ConversionService conversionService = datastore.mappingContext.conversionService
            if (args?.max) {
                q.setMaxResults(conversionService.convert(args.remove('max'), Integer))
            }
            if (args?.offset) {
                q.setFirstResult(conversionService.convert(args.remove('offset'), Integer))
            }
        }
    }
}
