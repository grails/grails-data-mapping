/*
 * Copyright 2014 the original author or authors.
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

import java.util.List;
import java.util.Map;

import grails.gorm.DetachedCriteria
import groovy.lang.Closure;
import groovy.transform.CompileStatic
/**
 * 
 * @author Jeff Brown
 * @since 4.0
 */
@CompileStatic
trait StaticQueryMethods<D> {
    
    /**
     *
     * @param callable Callable closure containing detached criteria definition
     * @return The DetachedCriteria instance
     */
    static DetachedCriteria<D> where(Closure callable) {
        new DetachedCriteria<D>(this).build(callable)
    }

    /**
     *
     * @param callable Callable closure containing detached criteria definition
     * @return The DetachedCriteria instance that is lazily initialized
     */
    static DetachedCriteria<D> whereLazy(Closure callable) {
        new DetachedCriteria<D>(this).buildLazy(callable)
    }
    
    /**
     *
     * @param callable Callable closure containing detached criteria definition
     * @return The DetachedCriteria instance
     */
    static DetachedCriteria<D> whereAny(Closure callable) {
        (DetachedCriteria<D>)new DetachedCriteria<D>(this).or(callable)
    }
    
    /**
     * Uses detached criteria to build a query and then execute it returning a list
     *
     * @param callable The callable
     * @return A List of entities
     */
    static List<D> findAll(Closure callable) {
        def criteria = new DetachedCriteria<D>(this).build(callable)
        criteria.list()
    }

    /**
     * Uses detached criteria to build a query and then execute it returning a list
     *
     * @param args pagination parameters
     * @param callable The callable
     * @return A List of entities
     */
    static List<D> findAll(Map args, Closure callable) {
        def criteria = new DetachedCriteria<D>(this).build(callable)
        criteria.list(args)
    }

    /**
     * Uses detached criteria to build a query and then execute it returning a list
     *
     * @param callable The callable
     * @return A single entity
     */
    static D find(Closure callable) {
        def criteria = new DetachedCriteria<D>(this).build(callable)
        criteria.find()
    }
}
