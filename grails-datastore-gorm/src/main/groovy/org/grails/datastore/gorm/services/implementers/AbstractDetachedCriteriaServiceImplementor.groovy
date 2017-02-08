package org.grails.datastore.gorm.services.implementers

import grails.gorm.DetachedCriteria
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.grails.datastore.mapping.model.config.GormProperties
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
        Parameter[] parameters = newMethodNode.parameters
        int parameterCount = parameters.length
        if(parameterCount == 1 && parameters[0].name == GormProperties.IDENTITY) {
            // optimize query by id
            Expression byId = callX( classX(domainClassNode), "get", varX(parameters[0]))
            implementById(domainClassNode,abstractMethodNode,newMethodNode, targetClassNode, body, byId)
        }
        else {
            Expression argsExpression = null
            VariableExpression queryVar = varX('$query')
            // def query = new DetachedCriteria(Foo)
            body.addStatement(
                    declS(queryVar, ctorX(ClassHelper.make(DetachedCriteria), args(classX(domainClassNode.plainNodeReference))))
            )

            if (parameterCount > 0) {
                for (Parameter parameter in parameters) {
                    String parameterName = parameter.name
                    if(parameterName == GormProperties.IDENTITY) {
                        body.addStatement(
                                stmt(
                                        callX(queryVar, "idEq", varX(parameter))
                                )
                        )
                    }
                    else if (isValidParameter(domainClassNode, parameter, parameterName)) {
                        body.addStatement(
                                stmt(
                                        callX(queryVar, "eq", args( constX(parameterName), varX(parameter) ))
                                )
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

                implementWithQuery(domainClassNode, abstractMethodNode, newMethodNode, targetClassNode, body, queryVar, argsExpression)
            }
        }
    }

    /**
     * Provide an implementation in the case querying for a single instance by id
     *
     * @param domainClassNode The domain class
     * @param abstractMethodNode the abstract method
     * @param newMethodNode The newly added method
     * @param targetClassNode The target class
     * @param body The body
     * @param byIdLookup The expression that looks up the object by id
     */
    abstract void implementById(ClassNode domainClassNode, MethodNode abstractMethodNode, MethodNode newMethodNode, ClassNode targetClassNode, BlockStatement body, Expression byIdLookup)

    /**
     * Provide an implementation in the case of a query
     *
     * @param domainClassNode The domain class
     * @param abstractMethodNode the abstract method
     * @param newMethodNode The newly added method
     * @param targetClassNode The target class
     * @param body The body
     * @param detachedCriteriaVar The detached criteria query
     * @param queryArgs Any arguments to the query
     */
    abstract void implementWithQuery(ClassNode domainClassNode, MethodNode abstractMethodNode, MethodNode newMethodNode, ClassNode targetClassNode, BlockStatement body, VariableExpression detachedCriteriaVar, Expression queryArgs)
}
