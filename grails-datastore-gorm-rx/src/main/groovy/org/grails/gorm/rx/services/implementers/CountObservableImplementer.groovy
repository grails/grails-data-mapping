package org.grails.gorm.rx.services.implementers

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.grails.datastore.gorm.services.implementers.CountImplementer
import grails.gorm.rx.services.RxSchedule
import org.grails.gorm.rx.transform.RxScheduleIOTransformation

import static org.grails.gorm.rx.transform.RxAstUtils.*

/**
 * Rx version of {@link CountImplementer}
 */
@CompileStatic
class CountObservableImplementer extends CountImplementer {

    @Override
    protected boolean isCompatibleReturnType(ClassNode domainClass, MethodNode methodNode, ClassNode returnType, String prefix) {
        return isObservableOf(returnType, Number)
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
        currentDomainClassNode
    }

    @Override
    void implementWithQuery(ClassNode domainClassNode, MethodNode abstractMethodNode, MethodNode newMethodNode, ClassNode targetClassNode, BlockStatement body, VariableExpression detachedCriteriaVar, Expression queryArgs) {
        if(!isRxEntity(domainClassNode)) {
            def ann = addAnnotationOrGetExisting(newMethodNode, RxSchedule)
            ann.setMember(RxScheduleIOTransformation.ANN_SINGLE_RESULT, ConstantExpression.TRUE)
            newMethodNode.addAnnotation(ann)

        }
        super.implementWithQuery(domainClassNode, abstractMethodNode, newMethodNode, targetClassNode, body, detachedCriteriaVar, queryArgs)
    }

}
