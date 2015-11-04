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

import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.reflect.NameUtils
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

