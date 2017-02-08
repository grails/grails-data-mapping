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
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.grails.datastore.mapping.reflect.AstUtils

import static org.codehaus.groovy.ast.tools.GeneralUtils.*
import static org.grails.datastore.gorm.transform.AstMethodDispatchUtils.namedArgs

/**
 * Implementations saving new entities
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
class SaveImplementer extends AbstractWriteOperationImplementer {
    static final List<String> HANDLED_PREFIXES = ['save', 'store', 'persist']

    @Override
    boolean doesImplement(ClassNode domainClass, MethodNode methodNode) {
        if( methodNode.parameters.length == 0 ) {
            return false
        }
        else {
            return super.doesImplement(domainClass, methodNode)
        }
    }

    @Override
    void doImplement(ClassNode domainClassNode, MethodNode abstractMethodNode, MethodNode newMethodNode, ClassNode targetClassNode) {
        BlockStatement body = (BlockStatement) newMethodNode.getCode()
        Parameter[] parameters = newMethodNode.parameters
        int parameterCount = parameters.length
        if(parameterCount == 1 && AstUtils.isDomainClass(parameters[0].type)) {
            body.addStatement(
                returnS( callX( varX( parameters[0] ), "save", namedArgs(failOnError: ConstantExpression.TRUE) ) )
            )
        }
        else {
            Expression argsExpression = null
            VariableExpression entityVar = varX('$entity')
            body.addStatement(
                declS(entityVar, ctorX(domainClassNode))
            )

            for (Parameter parameter in parameters) {
                String parameterName = parameter.name
                if (isValidParameter(domainClassNode, parameter, parameterName)) {
                    body.addStatement(
                        assignS( propX(entityVar, parameterName), varX(parameter) )
                    )
                } else if (parameter.type == ClassHelper.MAP_TYPE && parameterName == 'args') {
                    argsExpression = varX(parameter)
                } else {
                    AstUtils.error(
                            abstractMethodNode.declaringClass.module.context,
                            abstractMethodNode,
                            "Cannot implement method for argument [${parameterName}]. No property exists on domain class [$domainClassNode.name]"
                    )
                }
            }
            Expression saveArgs
            if(argsExpression != null) {
                saveArgs = varX('$args')
                body.addStatement(
                    declS( saveArgs, namedArgs(failOnError: ConstantExpression.TRUE))
                )
                body.addStatement(
                    stmt( callX( saveArgs, "putAll", argsExpression) )
                )
            }
            else {
                saveArgs = namedArgs(failOnError: ConstantExpression.TRUE)
            }

            body.addStatement(
                returnS( callX( entityVar, "save", saveArgs ) )
            )
        }
    }

    @Override
    protected boolean isCompatibleReturnType(ClassNode returnType) {
        return AstUtils.isDomainClass(returnType)
    }

    @Override
    protected Iterable<String> getHandledPrefixes() {
        return HANDLED_PREFIXES
    }
}
