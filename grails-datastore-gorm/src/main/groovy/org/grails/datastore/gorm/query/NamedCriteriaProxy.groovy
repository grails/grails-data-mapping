/*
 * Copyright 2015 original authors
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
package org.grails.datastore.gorm.query

import grails.gorm.CriteriaBuilder
import org.grails.datastore.gorm.finders.DynamicFinder
import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.mapping.model.PersistentEntity

import java.lang.reflect.Modifier



/**
 * Handles named queries
 *
 * @author Graeme Rocher
 * @author Jeff Brown
 *
 *
 * @since 5.0
 */
class NamedCriteriaProxy<D> implements GormQueryOperations<D> {

    Closure criteriaClosure
    PersistentEntity entity
    List finders
    private namedCriteriaParams
    private previousInChain
    private queryBuilder

    private invokeCriteriaClosure(additionalCriteriaClosure = null) {
        def crit = getPreparedCriteriaClosure(additionalCriteriaClosure)
        crit()
    }

    def call(Object[] params) {
        if (params && params[-1] instanceof Closure) {
            Closure additionalCriteriaClosure = (Closure)params[-1]
            params = params.length > 1 ? params[0..-2] : [:]
            if (params) {
                if (params[-1] instanceof Map) {
                    if (params.length > 1) {
                        namedCriteriaParams = params[0..-2] as Object[]
                    }
                    return list((Map)params[-1], additionalCriteriaClosure)
                } else {
                    namedCriteriaParams = params
                    return list(Collections.emptyMap(), additionalCriteriaClosure)
                }
            }
            else {
                return list(Collections.emptyMap(), additionalCriteriaClosure)
            }
        }
        else {
            namedCriteriaParams = params
            this
        }
    }

    D get(Serializable id) {
        id = (Serializable)entity.mappingContext.conversionService.convert(id, entity.identity.type)
        def getClosure = {
            queryBuilder = delegate
            invokeCriteriaClosure()
            eq 'id', id
            uniqueResult = true
        }
        return  entity.javaClass.createCriteria().get(getClosure)
    }

    @Override
    D find(Map args = Collections.emptyMap(), Closure additionalCriteria = null) {
        return get(args, additionalCriteria)
    }

    D find(Closure additionalCriteria) {
        return get(Collections.emptyMap(), additionalCriteria)
    }

    D get(Closure additionalCriteria) {
        return get(Collections.emptyMap(), additionalCriteria)
    }

    @Override
    D get(Map paramsMap = Collections.emptyMap(), Closure additionalCriteria = null) {
        def conversionService = entity.mappingContext.conversionService
        return (D) entity.javaClass.createCriteria().get( {
            queryBuilder = delegate
            invokeCriteriaClosure(additionalCriteria)
            if (paramsMap?.max) {
                maxResults conversionService.convert(paramsMap.max, Integer)
            }
            if (paramsMap?.offset) {
                firstResult conversionService.convert(paramsMap.offset, Integer)
            }
            uniqueResult = true
            if (paramsMap && queryBuilder instanceof CriteriaBuilder) {
                DynamicFinder.populateArgumentsForCriteria(entity.javaClass, queryBuilder.query, paramsMap)
            }
        } )
    }


    List<D> list(Closure additionalCriteria) {
        list(Collections.emptyMap(), additionalCriteria)
    }

    @Override
    List<D> list(Map paramsMap = Collections.emptyMap(), Closure additionalCriteria = null) {
        def conversionService = entity.mappingContext.conversionService
        return entity.javaClass.withCriteria {
            queryBuilder = delegate
            invokeCriteriaClosure(additionalCriteria)
            if (paramsMap?.max) {
                maxResults conversionService.convert(paramsMap.max, Integer)
            }
            if (paramsMap?.offset) {
                firstResult conversionService.convert(paramsMap.offset, Integer)
            }
            if (paramsMap && queryBuilder instanceof CriteriaBuilder) {
                DynamicFinder.populateArgumentsForCriteria(entity.javaClass, queryBuilder.query, paramsMap)
            }
        }
    }

    @Override
    Number count(Map args = Collections.emptyMap(), Closure additionalCriteria = null) {
        def countClosure = {
            queryBuilder = delegate
            invokeCriteriaClosure(additionalCriteria)
        }
        entity.javaClass.createCriteria().count(countClosure)
    }

    Number count(Closure additionalCriteria ) {
        count(Collections.emptyMap(), additionalCriteria)
    }


    D findWhere(Map params) {
        def queryClosure = {
            queryBuilder = delegate
            invokeCriteriaClosure()
            params.each {key, val ->
                eq key, val
            }
            maxResults 1
            uniqueResult = true
        }
        entity.javaClass.withCriteria(queryClosure)
    }

    List<D> findAllWhere(Map params) {
        def queryClosure = {
            queryBuilder = delegate
            invokeCriteriaClosure()
            params.each {key, val ->
                eq key, val
            }
        }
        entity.javaClass.withCriteria(queryClosure)
    }

    def propertyMissing(String propertyName) {
        def javaClass = entity.javaClass
        if (javaClass.metaClass.getMetaProperty(propertyName)) {
            def nextInChain = javaClass.metaClass.getMetaProperty(propertyName).getProperty(javaClass)
            nextInChain.previousInChain = this
            return nextInChain
        }
        throw new MissingPropertyException(propertyName, NamedCriteriaProxy)
    }

    void propertyMissing(String propName, val) {
        queryBuilder?."${propName}" = val
    }

    def methodMissing(String methodName, args) {

        def javaClass = entity.javaClass
        FinderMethod method = finders.find { FinderMethod f ->  f.isMethodMatch(methodName) }

        if (method) {
            def preparedClosure = getPreparedCriteriaClosure()
            return method.invoke(javaClass, methodName, preparedClosure, args)
        }

        if (!queryBuilder && javaClass.metaClass.getMetaProperty(methodName)) {
            def nextInChain = javaClass.metaClass.getMetaProperty(methodName).getProperty(entity)
            nextInChain.previousInChain = this
            return nextInChain(args)
        }

        def metaProperty = javaClass.metaClass.getMetaProperty(methodName)
        if (metaProperty && Modifier.isStatic(metaProperty.modifiers)) {
            def staticProperty = metaProperty.getProperty(javaClass)
            if (staticProperty instanceof NamedCriteriaProxy) {
                def nestedCriteria = staticProperty.criteriaClosure.clone()
                nestedCriteria.delegate = queryBuilder
                return nestedCriteria(*args)
            }
        }
        queryBuilder."${methodName}"(*args)
    }

    private getPreparedCriteriaClosure(additionalCriteriaClosure = null) {
        Closure closureClone = criteriaClosure.clone()
        closureClone.resolveStrategy = Closure.DELEGATE_FIRST
        if (namedCriteriaParams) {
            closureClone = closureClone.curry(*namedCriteriaParams)
        }
        def c = {
            closureClone.delegate = delegate
            if (previousInChain) {
                def previousClosure = previousInChain.getPreparedCriteriaClosure()
                previousClosure.delegate = delegate
                previousClosure()
            }
            closureClone()
            if (additionalCriteriaClosure) {
                additionalCriteriaClosure = additionalCriteriaClosure.clone()
                additionalCriteriaClosure.delegate = delegate
                additionalCriteriaClosure()
            }
        }
        c
    }


}
