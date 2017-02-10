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
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.grails.datastore.mapping.reflect.AstUtils
import static org.codehaus.groovy.ast.tools.GeneralUtils.*
/**
 * An implementer that implements logic for finding a single entity
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
class FindOneImplementer extends AbstractDetachedCriteriaServiceImplementor {
    static final List<String> HANDLED_PREFIXES = ['retrieve','get', 'find', 'read']

    @Override
    protected boolean isCompatibleReturnType(ClassNode domainClass, MethodNode methodNode, ClassNode returnType, String prefix) {
        return AstUtils.isDomainClass(returnType) && !returnType.isArray()
    }

    @Override
    Iterable<String> getHandledPrefixes() {
        return HANDLED_PREFIXES
    }

    @Override
    protected ClassNode resolveDomainClassFromSignature(ClassNode currentDomainClassNode, MethodNode methodNode) {
        return methodNode.returnType
    }

    @Override
    void implementById(ClassNode domainClassNode, MethodNode abstractMethodNode, MethodNode newMethodNode, ClassNode targetClassNode, BlockStatement body, Expression byIdLookup) {
        body.addStatement(
            buildReturnStatement(domainClassNode, null, byIdLookup)
        )
    }

    @Override
    void implementWithQuery(ClassNode domainClassNode, MethodNode abstractMethodNode, MethodNode newMethodNode, ClassNode targetClassNode, BlockStatement body, VariableExpression detachedCriteriaVar, Expression queryArgs) {
        MethodCallExpression findCall = queryArgs != null ? callX(detachedCriteriaVar, "find", queryArgs) : callX(detachedCriteriaVar, "find")
        body.addStatement(
            buildReturnStatement(domainClassNode, queryArgs, findCall)
        )
    }

    protected Statement buildReturnStatement(ClassNode targetDomainClass, Expression args, Expression queryMethodCall) {
        returnS(
            queryMethodCall
        )
    }
}
