package org.grails.gorm.rx.services.implementers

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.stmt.Statement
import org.grails.datastore.gorm.services.implementers.FindOneImplementer
import org.grails.gorm.rx.transform.RxScheduleIOTransformation
import static org.grails.gorm.rx.transform.RxAstUtils.*
/**
 * Implements a service method that returns an Observable or a Single
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
class FindOneObservableImplementer extends FindOneImplementer {

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
            newMethodNode.addAnnotation(RxScheduleIOTransformation.ANNOTATION)
            return super.buildReturnStatement(targetDomainClass, abstractMethodNode, queryMethodCall, queryArgs, newMethodNode)
        }
    }

    @Override
    protected String findMethodToInvoke(ClassNode domainClassNode, MethodNode newMethodNode) {
        if(isSingle(newMethodNode.returnType)) {
            return "get"
        }
        else if(isRxEntity(domainClassNode)) {
            return "findAll"
        }
        else {
            return "list"
        }
    }
}
