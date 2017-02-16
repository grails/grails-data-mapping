package org.grails.gorm.rx.services.implementers

import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.grails.datastore.gorm.services.implementers.DeleteImplementer
import org.grails.gorm.rx.transform.RxScheduleIOTransformation

import static org.grails.gorm.rx.transform.RxAstUtils.*

/**
 * Rx version of {@link DeleteImplementer}
 *
 * @author Graeme Rocher
 * @since 6.1
 */
class DeleteObservableImplementer extends DeleteImplementer {


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
            def ann = new AnnotationNode(RxScheduleIOTransformation.ANNOTATION_TYPE)
            ann.setMember(RxScheduleIOTransformation.ANN_SINGLE_RESULT, ConstantExpression.TRUE)
            newMethodNode.addAnnotation(ann)

        }
        super.implementWithQuery(domainClassNode, abstractMethodNode, newMethodNode, targetClassNode, body, detachedCriteriaVar, queryArgs)
    }
}
