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

package org.grails.datastore.gorm.query

import grails.gorm.CriteriaBuilder;

import java.lang.reflect.Modifier

import org.codehaus.groovy.runtime.MetaClassHelper;
import org.grails.datastore.gorm.finders.DynamicFinder
import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.reflect.NameUtils;

/**
 * Handles creation of named queries
 *
 * @author Graeme Rocher
 * @author Jeff Brown
 */
class NamedQueriesBuilder {
    PersistentEntity entity
    List<FinderMethod> finders
    boolean initialized = false

    NamedQueriesBuilder(PersistentEntity entity, List<FinderMethod> finders) {
        this.entity = entity
        this.finders = finders
    }

    def evaluate(Closure namedQueriesClosure) {
        def closure = namedQueriesClosure.clone()
        closure.resolveStrategy = Closure.DELEGATE_ONLY
        closure.delegate = this
        closure.call()
        initialized = true
    }

    protected def handleMethodMissing(String name, args) {
        Closure criteriaClosure = args[0]
        def getterName = NameUtils.getGetterName(name)
        classesToAugment.each { clz ->
            def methodMissingClosure = {->
                // creating a new proxy each time because the proxy class has
                // some state that cannot be shared across requests (namedCriteriaParams)
                createNamedCriteriaProxy(criteriaClosure)
            }
            clz.metaClass.static."${getterName}" = methodMissingClosure
        }
        null
    }
    
    protected List<Class<?>> getClassesToAugment() {
        [entity.javaClass]
    }
    
    protected def createNamedCriteriaProxy(Closure criteriaClosure) {
        new NamedCriteriaProxy(criteriaClosure: criteriaClosure, entity: entity, finders: finders)
    }

    def methodMissing(String name, args) {
        if (args && args[0] instanceof Closure && !initialized) {
            return handleMethodMissing(name, args)
        }
        throw new MissingMethodException(name, NamedQueriesBuilder, args)
    }
}

class NamedCriteriaProxy {

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

    private listInternal(Object[] params, Closure additionalCriteriaClosure, Boolean isDistinct) {
        def conversionService = entity.mappingContext.conversionService
        def listClosure = {
            queryBuilder = delegate
            invokeCriteriaClosure(additionalCriteriaClosure)
            def paramsMap
            if (params && params[-1] instanceof Map) {
                paramsMap = params[-1]
            }
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
        entity.javaClass.withCriteria(listClosure)
    }

    def list(Object[] params, Closure additionalCriteriaClosure = null) {
        listInternal params, additionalCriteriaClosure, false
    }

    def listDistinct(Object[] params, Closure additionalCriteriaClosure = null) {
        listInternal params, additionalCriteriaClosure, true
    }

    def call(Object[] params) {
        if (params && params[-1] instanceof Closure) {
            def additionalCriteriaClosure = params[-1]
            params = params.length > 1 ? params[0..-2] : [:]
            if (params) {
                if (params[-1] instanceof Map) {
                    if (params.length > 1) {
                        namedCriteriaParams = params[0..-2] as Object[]
                    }
                } else {
                    namedCriteriaParams = params
                }
            }
            list(params, additionalCriteriaClosure)
        }
        else {
            namedCriteriaParams = params
            this
        }
    }

    def get(id) {
        id = entity.mappingContext.conversionService.convert(id, entity.identity.type)
        def getClosure = {
            queryBuilder = delegate
            invokeCriteriaClosure()
            eq 'id', id
            uniqueResult = true
        }
        entity.javaClass.createCriteria().get(getClosure)
    }

    def count(Closure additionalCriteriaClosure = null) {
        def countClosure = {
            queryBuilder = delegate
            invokeCriteriaClosure(additionalCriteriaClosure)
        }
        entity.javaClass.createCriteria().count(countClosure)
    }

    def findWhere(params) {
        findAllWhere(params, true)
    }

    def findAllWhere(Map params, Boolean uniq = false) {
        def queryClosure = {
            queryBuilder = delegate
            invokeCriteriaClosure()
            params.each {key, val ->
                eq key, val
            }
            if (uniq) {
                maxResults 1
                uniqueResult = true
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
