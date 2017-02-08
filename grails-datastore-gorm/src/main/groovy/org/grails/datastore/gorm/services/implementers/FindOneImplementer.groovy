/*
 * Copyright 2017 the original author or authors.
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
package org.grails.datastore.gorm.services.implementers

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ClassNode
import org.grails.datastore.mapping.reflect.AstUtils

/**
 * An implementer that implements logic for finding a single entity
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
class FindOneImplementer extends FindAllImplementer {
    static final List<String> HANDLED_PREFIXES = ['retrieve','get', 'find']

    @Override
    protected boolean isCompatibleReturnType(ClassNode returnType) {
        return AstUtils.isDomainClass(returnType) && !returnType.isArray()
    }

    @Override
    protected ClassNode resolveDomainClassForReturnType(ClassNode returnType, boolean isArray) {
        return returnType
    }

    @Override
    protected String getNoArgumentsMethodName() {
        return "first"
    }

    @Override
    protected String getQueryMethodName() {
        return "findWhere"
    }

    @Override
    Iterable<String> getHandledPrefixes() {
        return HANDLED_PREFIXES
    }
}
