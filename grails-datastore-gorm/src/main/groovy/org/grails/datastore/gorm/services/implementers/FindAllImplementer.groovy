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
import org.codehaus.groovy.ast.PropertyNode
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MapEntryExpression
import org.codehaus.groovy.ast.expr.MapExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.grails.datastore.mapping.core.Ordered
import org.grails.datastore.mapping.model.config.GormProperties
import org.grails.datastore.mapping.reflect.AstUtils

import static org.codehaus.groovy.ast.tools.GeneralUtils.*

/**
 * Automatically implements {@link grails.gorm.services.Service} interface methods that start with "list" and
 * return an iterable of domain classes
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
class FindAllImplementer extends AbstractArrayOrIterableResultImplementer implements Ordered {

    static final List<String> HANDLED_PREFIXES = ['list', 'find', 'get', 'retrieve']

    @Override
    Iterable<String> getHandledPrefixes() {
        HANDLED_PREFIXES
    }

    @Override
    void doImplement(ClassNode domainClassNode, ClassNode targetClassNode, MethodNode abstractMethodNode, MethodNode newMethodNode, boolean isArray) {
        BlockStatement body = (BlockStatement)newMethodNode.getCode()
        Expression argsToMethod = AstUtils.ZERO_ARGUMENTS
        String methodToInvoke = getNoArgumentsMethodName()
        Parameter[] parameters = newMethodNode.parameters
        int parameterCount = parameters.length
        Expression argsExpression = null
        if(parameterCount > 0) {
            if(parameterCount == 1) {
                Parameter parameter = parameters[0]
                String parameterName = parameter.name
                if(parameter.type == ClassHelper.MAP_TYPE && parameterName == 'args') {
                    argsToMethod = args( parameters )
                }
                else {
                    if(isValidParameter(domainClassNode, parameter, parameterName)) {
                        methodToInvoke = getQueryMethodName()
                        argsToMethod = args( new MapExpression([new MapEntryExpression(
                            constX(parameterName),
                            varX(parameter)
                        )]) )
                    }
                    else {
                        AstUtils.error(
                            abstractMethodNode.declaringClass.module.context,
                            abstractMethodNode,
                            "Cannot implement method for argument [${parameterName}]. No property exists on domain class [$domainClassNode.name]"
                        )
                    }
                }
            }
            else {
                methodToInvoke = getQueryMethodName()
                List<MapEntryExpression> queryParameters = []
                for(Parameter parameter in parameters) {
                    if(domainClassNode.hasProperty(parameter.name)) {
                        queryParameters.add new MapEntryExpression(
                                constX(parameter.name),
                                varX(parameter)
                        )
                    }
                    else if(parameter.type == ClassHelper.MAP_TYPE && parameter.name == 'args') {
                        argsExpression = varX( parameter )
                    }
                    else {
                        AstUtils.error(
                                abstractMethodNode.declaringClass.module.context,
                                abstractMethodNode,
                                "Cannot implement method for argument [$parameter.name]. No property exists on domain class [$domainClassNode.name]"
                        )
                    }
                }
                argsToMethod = argsExpression != null ? args( new MapExpression(queryParameters), argsExpression ) : args( new MapExpression(queryParameters) )
            }
        }
        // add a method that invokes list()
        Expression queryMethodCall = callX(classX(domainClassNode.plainNodeReference), methodToInvoke, argsToMethod)
        if(isArray) {
            // handle array cast
            ClassNode returnType = newMethodNode.returnType
            queryMethodCall = castX( returnType.plainNodeReference, queryMethodCall )
        }
        body.addStatement(
            buildReturnStatement(queryMethodCall, argsExpression)
        )
    }


    protected Statement buildReturnStatement(Expression queryMethodCall, Expression args) {
        returnS queryMethodCall
    }

    protected String getQueryMethodName() {
        'findAllWhere'
    }

    protected String getNoArgumentsMethodName() {
        "list"
    }

}
