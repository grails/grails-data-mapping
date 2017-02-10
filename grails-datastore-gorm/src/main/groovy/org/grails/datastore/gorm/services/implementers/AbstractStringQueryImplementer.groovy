package org.grails.datastore.gorm.services.implementers

import grails.gorm.services.Query
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.VariableScope
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.GStringExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.grails.datastore.gorm.services.transform.QueryStringTransformer
import org.grails.datastore.mapping.reflect.AstUtils

import static org.codehaus.groovy.ast.tools.GeneralUtils.args
import static org.codehaus.groovy.ast.tools.GeneralUtils.constX

/**
 * Abstract support for String-based queries
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
abstract class AbstractStringQueryImplementer extends AbstractReadOperationImplementer {
    @Override
    int getOrder() {
        return FindByImplementer.POSITION - 100
    }

    @Override
    boolean doesImplement(ClassNode domainClass, MethodNode methodNode) {
        if( AstUtils.findAnnotation(methodNode, Query) != null) {
            return isCompatibleReturnType(domainClass, methodNode, methodNode.returnType, methodNode.name)
        }
        return false
    }

    @Override
    void doImplement(ClassNode domainClassNode, MethodNode abstractMethodNode, MethodNode newMethodNode, ClassNode targetClassNode) {
        AnnotationNode annotationNode = AstUtils.findAnnotation(abstractMethodNode, Query)
        Expression expr = annotationNode.getMember("value")
        VariableScope scope = newMethodNode.variableScope
        if(expr instanceof GStringExpression) {
            GStringExpression gstring = (GStringExpression)expr
            QueryStringTransformer transformer = new QueryStringTransformer(abstractMethodNode.declaringClass.module.context, scope)
            Expression transformed = transformer.transform(gstring)
            BlockStatement body = (BlockStatement)newMethodNode.code
            Expression argMap = findArgsExpression(newMethodNode)
            if(argMap != null) {
                transformed = args( transformed, argMap )
            }
            body.addStatement(
                buildQueryReturnStatement(domainClassNode, newMethodNode, transformed)
            )
            annotationNode.setMember("value", constX("IMPLEMENTED"))
        }
    }

    /**
     * Builds the query return statement
     *
     * @param domainClassNode The domain class
     * @param args The arguments
     * @return The statement
     */
    protected abstract Statement buildQueryReturnStatement(ClassNode domainClassNode, MethodNode newMethodNode, Expression args)

    @Override
    protected Iterable<String> getHandledPrefixes() {
        return Collections.emptyList()
    }
}
