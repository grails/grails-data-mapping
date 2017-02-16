package org.grails.gorm.rx.services.implementers

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.stmt.Statement
import org.grails.datastore.gorm.services.implementers.CountByImplementer
import org.grails.gorm.rx.transform.RxScheduleIOTransformation

import static org.grails.gorm.rx.transform.RxAstUtils.isObservableOf
import static org.grails.gorm.rx.transform.RxAstUtils.isRxEntity

/**
 * Rx version of {@link CountByImplementer}
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
class CountByObservableImplementer extends CountByImplementer {

    @Override
    protected boolean isCompatibleReturnType(ClassNode domainClass, MethodNode methodNode, ClassNode returnType, String prefix) {
        return isObservableOf(returnType, Number)
    }

    @Override
    protected ClassNode resolveDomainClassForReturnType(ClassNode currentDomainClass, boolean isArray, ClassNode returnType) {
        currentDomainClass
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
