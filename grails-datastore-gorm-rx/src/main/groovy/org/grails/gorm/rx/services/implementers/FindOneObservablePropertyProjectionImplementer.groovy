package org.grails.gorm.rx.services.implementers

import grails.gorm.rx.services.RxSchedule
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.grails.datastore.gorm.services.implementers.FindOnePropertyProjectionImplementer
import static org.grails.gorm.rx.transform.RxAstUtils.*


/**
 * Rx version of {@link FindOnePropertyProjectionImplementer}
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
class FindOneObservablePropertyProjectionImplementer extends FindOnePropertyProjectionImplementer {

    @Override
    protected boolean isCompatibleReturnType(ClassNode domainClass, MethodNode methodNode, ClassNode returnType, String prefix) {
        if( isObservable(returnType) || isSingle(returnType) ) {
            if(returnType.genericsTypes != null && returnType.genericsTypes.length > 0) {
                return super.isCompatibleReturnType(domainClass, methodNode, returnType, prefix)
            }
        }
        return false
    }

    @Override
    protected ClassNode resolveProjectionReturnType(ClassNode returnType) {
        return returnType.genericsTypes[0].type
    }

    @Override
    void implementWithQuery(ClassNode domainClassNode, MethodNode abstractMethodNode, MethodNode newMethodNode, ClassNode targetClassNode, BlockStatement body, VariableExpression detachedCriteriaVar, Expression queryArgs) {
        addAnnotationOrGetExisting(newMethodNode, RxSchedule)
        super.implementWithQuery(domainClassNode, abstractMethodNode, newMethodNode, targetClassNode, body, detachedCriteriaVar, queryArgs)
    }

    @Override
    protected String getQueryMethodToInvoke(ClassNode domainClassNode, MethodNode newMethodNode) {
        if(isSingle(newMethodNode.returnType)) {
            return "get"
        }
        else {
            return "list"
        }
    }
}
