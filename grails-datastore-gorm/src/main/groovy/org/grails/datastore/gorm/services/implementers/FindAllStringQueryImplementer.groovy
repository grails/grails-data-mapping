package org.grails.datastore.gorm.services.implementers

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression
import org.codehaus.groovy.ast.stmt.Statement

import static org.codehaus.groovy.ast.tools.GeneralUtils.callX
import static org.codehaus.groovy.ast.tools.GeneralUtils.castX
import static org.codehaus.groovy.ast.tools.GeneralUtils.returnS

/**
 * Implements support for String-based queries that return an iterable or array of domain classes
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
class FindAllStringQueryImplementer extends AbstractStringQueryImplementer {
    @Override
    protected boolean isCompatibleReturnType(ClassNode domainClass, MethodNode methodNode, ClassNode returnType, String prefix) {
        return AbstractArrayOrIterableResultImplementer.isIterableOrArrayOfDomainClasses(returnType)
    }

    @Override
    protected Statement buildQueryReturnStatement(ClassNode domainClassNode, MethodNode newMethodNode, Expression args) {
        ClassNode returnType = newMethodNode.returnType
        Expression methodCall = callX(domainClassNode, "findAll", args)
        if(returnType.isArray()) {
            methodCall = castX(returnType.plainNodeReference, methodCall)
        }
        return returnS(methodCall)
    }
}
