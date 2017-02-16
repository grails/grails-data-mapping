package org.grails.gorm.rx.services.implementers

import grails.gorm.rx.services.RxSchedule
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.stmt.Statement
import org.grails.datastore.gorm.services.implementers.FindAndDeleteImplementer

import static org.grails.datastore.mapping.reflect.AstUtils.addAnnotationOrGetExisting
import static org.grails.gorm.rx.transform.RxAstUtils.*

/**
 * Rx version of {@link FindAndDeleteImplementer}
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
class FindAndDeleteObservableImplementer extends FindAndDeleteImplementer {
    @Override
    protected boolean isCompatibleReturnType(ClassNode domainClass, MethodNode methodNode, ClassNode returnType, String prefix) {
        return isObservableOfDomainClass(returnType)
    }

    @Override
    protected ClassNode getDetachedCriteriaType(ClassNode domainClassNode) {
        if(isRxEntity(domainClassNode)) {
            return RX_DETACHED_CRITERIA
        }
        else {
            return super.getDetachedCriteriaType(domainClassNode)
        }
    }

    @Override
    protected ClassNode resolveDomainClassFromSignature(ClassNode currentDomainClassNode, MethodNode methodNode) {
        methodNode.returnType.genericsTypes[0].type
    }

    @Override
    protected Statement buildReturnStatement(ClassNode targetDomainClass, MethodNode abstractMethodNode, Expression queryMethodCall, Expression queryArgs, MethodNode newMethodNode) {
        // if it is an RxEntity then the method call will return an observable, so simply return
        if(isRxEntity(targetDomainClass)) {
            return super.buildReturnStatement(targetDomainClass, abstractMethodNode, queryMethodCall, queryArgs, newMethodNode)
        }
        else {
            addAnnotationOrGetExisting(newMethodNode, RxSchedule)
            return super.buildReturnStatement(targetDomainClass, abstractMethodNode, queryMethodCall, queryArgs, newMethodNode)
        }
    }

    @Override
    protected String findMethodToInvoke(ClassNode domainClassNode, MethodNode newMethodNode) {
        return "get"
    }
}
