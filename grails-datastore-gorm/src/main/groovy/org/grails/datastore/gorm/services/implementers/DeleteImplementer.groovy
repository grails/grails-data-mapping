package org.grails.datastore.gorm.services.implementers

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.grails.datastore.gorm.transactions.transform.TransactionalTransform
import org.grails.datastore.mapping.reflect.AstUtils

import static org.codehaus.groovy.ast.tools.GeneralUtils.*

/**
 * Implements "void delete(..)"
 *
 * @author Graeme Rocher
 */
@CompileStatic
class DeleteImplementer extends AbstractDetachedCriteriaServiceImplementor {
    static final List<String> HANDLED_PREFIXES = ['delete', 'remove']

    @Override
    boolean doesImplement(ClassNode domainClass, MethodNode methodNode) {
        if(methodNode.parameters.length == 0) return false
        else {
            return AstUtils.isDomainClass(domainClass) && super.doesImplement(domainClass, methodNode)
        }
    }

    @Override
    protected boolean isCompatibleReturnType(ClassNode returnType) {
        return ClassHelper.VOID_TYPE.equals(returnType) || AstUtils.implementsInterface(returnType, "java.lang.Number")
    }

    @Override
    protected Iterable<String> getHandledPrefixes() {
        return HANDLED_PREFIXES
    }

    @Override
    protected void applyDefaultTransactionHandling(MethodNode newMethodNode) {
        newMethodNode.addAnnotation(new AnnotationNode(TransactionalTransform.MY_TYPE))
    }

    @Override
    void implementById(ClassNode domainClassNode, MethodNode abstractMethodNode, MethodNode newMethodNode, ClassNode targetClassNode, BlockStatement body, Expression byIdLookup) {
        boolean isVoidReturnType = ClassHelper.VOID_TYPE.equals(newMethodNode.returnType)
        VariableExpression obj = varX('$obj')
        Statement deleteStatement = stmt(callX(obj, "delete"))
        if(!isVoidReturnType) {
            deleteStatement = block(
                deleteStatement,
                returnS(constX(1))
            )
        }

        body.addStatements([
            declS(obj, byIdLookup),
            ifS(
                notNullX(obj),
                deleteStatement
            )
        ])
        if(!isVoidReturnType) {
            body.addStatement(
                returnS(constX(0))
            )
        }
    }

    @Override
    void implementWithQuery(ClassNode domainClassNode, MethodNode abstractMethodNode, MethodNode newMethodNode, ClassNode targetClassNode, BlockStatement body, VariableExpression detachedCriteriaVar, Expression queryArgs) {

        MethodCallExpression deleteCall = callX(detachedCriteriaVar, "deleteAll" )
        boolean isVoidReturnType = ClassHelper.VOID_TYPE.equals(newMethodNode.returnType)

        body.addStatements([
                // return query.deleteAll()
                isVoidReturnType ? stmt(deleteCall) : returnS( deleteCall )
        ])
    }
}
