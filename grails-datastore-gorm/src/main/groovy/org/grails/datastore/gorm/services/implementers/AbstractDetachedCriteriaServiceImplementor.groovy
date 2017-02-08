package org.grails.datastore.gorm.services.implementers

import grails.gorm.DetachedCriteria
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MapEntryExpression
import org.codehaus.groovy.ast.expr.MapExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.grails.datastore.mapping.reflect.AstUtils

import static org.codehaus.groovy.ast.tools.GeneralUtils.*

/**
 * An abstract implementer that builds a detached criteria query from the method arguments
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
abstract class AbstractDetachedCriteriaServiceImplementor extends AbstractReadOperationImplementer {
    @Override
    void doImplement(ClassNode domainClassNode, MethodNode abstractMethodNode, MethodNode newMethodNode, ClassNode targetClassNode) {
        BlockStatement body = (BlockStatement) newMethodNode.getCode()
        Expression argsToMethod = AstUtils.ZERO_ARGUMENTS
        Parameter[] parameters = newMethodNode.parameters
        int parameterCount = parameters.length
        Expression argsExpression = null

        if (parameterCount > 0) {
            if (parameterCount == 1) {
                Parameter parameter = parameters[0]
                String parameterName = parameter.name
                if (parameter.type == ClassHelper.MAP_TYPE && parameterName == 'args') {
                    argsToMethod = args(parameters)
                } else {
                    if (isValidParameter(domainClassNode, parameter, parameterName)) {
                        argsToMethod = args(new MapExpression([new MapEntryExpression(
                                constX(parameterName),
                                varX(parameter)
                        )]))
                    } else {
                        AstUtils.error(
                                abstractMethodNode.declaringClass.module.context,
                                abstractMethodNode,
                                "Cannot implement method for argument [${parameterName}]. No property exists on domain class [$domainClassNode.name]"
                        )
                    }
                }
            } else {
                List<MapEntryExpression> queryParameters = []
                for (Parameter parameter in parameters) {
                    String parameterName = parameter.name
                    if (isValidParameter(domainClassNode, parameter, parameterName)) {
                        queryParameters.add new MapEntryExpression(
                                constX(parameterName),
                                varX(parameter)
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
                argsToMethod = argsExpression != null ? args(new MapExpression(queryParameters), argsExpression) : args(new MapExpression(queryParameters))
            }

            VariableExpression queryVar = varX('$query')
            body.addStatements([
                    // def query = new DetachedCriteria(Foo)
                    declS(queryVar, ctorX(ClassHelper.make(DetachedCriteria), args(classX(domainClassNode.plainNodeReference)))),
                    // query.allEq( criteria )
                    stmt(callX(queryVar, "allEq", argsToMethod))
            ])

            implementWithQuery(domainClassNode, abstractMethodNode, newMethodNode, targetClassNode, body, queryVar)
        }
    }

    abstract void implementWithQuery(ClassNode domainClassNode,
                                     MethodNode abstractMethodNode,
                                     MethodNode newMethodNode,
                                     ClassNode targetClassNode,
                                     BlockStatement body,
                                     VariableExpression detachedCriteriaVar)
}
