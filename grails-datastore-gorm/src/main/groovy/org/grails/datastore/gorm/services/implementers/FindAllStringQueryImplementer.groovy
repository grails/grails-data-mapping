package org.grails.datastore.gorm.services.implementers

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.GenericsType
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression
import org.codehaus.groovy.ast.stmt.Statement

import static org.codehaus.groovy.ast.tools.GeneralUtils.callX
import static org.codehaus.groovy.ast.tools.GeneralUtils.castX
import static org.codehaus.groovy.ast.tools.GeneralUtils.returnS
import static org.grails.datastore.mapping.reflect.AstUtils.implementsInterface
import static org.grails.datastore.mapping.reflect.AstUtils.isDomainClass
import static org.grails.datastore.mapping.reflect.AstUtils.isDomainClass

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
        boolean isCompatibleReturnType = false
        if (returnType.name == Iterable.name || implementsInterface(returnType, Iterable.name)) {
            isCompatibleReturnType = true
        } else if (returnType.isArray()) {
            isCompatibleReturnType = true
        }
        return isCompatibleReturnType
    }

    @Override
    protected Statement buildQueryReturnStatement(ClassNode domainClassNode, MethodNode newMethodNode, Expression args) {
        ClassNode returnType = newMethodNode.returnType
        Expression methodCall = callX(domainClassNode, "findAll", args)
        methodCall = castX(returnType.plainNodeReference, methodCall)
        return returnS(methodCall)
    }
}
