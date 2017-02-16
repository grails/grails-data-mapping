package org.grails.gorm.rx.services.implementers

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.stmt.Statement
import org.grails.datastore.gorm.services.implementers.CountWhereImplementer
import org.grails.gorm.rx.transform.RxScheduleIOTransformation

import static org.grails.gorm.rx.transform.RxAstUtils.*

/**
 * Rx version of {@link CountWhereImplementer}
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
class CountWhereObservableImplementer extends CountWhereImplementer {

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
    protected Statement buildReturnStatement(ClassNode domainClass, MethodNode abstractMethodNode, MethodNode newMethodNode, Expression queryExpression) {
        if(!isRxEntity(domainClass)) {
            def ann = new AnnotationNode(RxScheduleIOTransformation.ANNOTATION_TYPE)
            ann.setMember(RxScheduleIOTransformation.ANN_SINGLE_RESULT, ConstantExpression.TRUE)
            newMethodNode.addAnnotation(ann)

        }
        return super.buildReturnStatement(domainClass, abstractMethodNode, newMethodNode, queryExpression)
    }
}
