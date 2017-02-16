package org.grails.gorm.rx.services.implementers

import grails.gorm.rx.services.RxSchedule
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.stmt.Statement
import org.grails.datastore.gorm.services.implementers.FindByImplementer
import org.grails.datastore.gorm.services.implementers.FindOneByImplementer
import org.grails.datastore.mapping.reflect.AstUtils
import org.grails.gorm.rx.transform.RxScheduleIOTransformation

import static org.grails.gorm.rx.transform.RxAstUtils.isObservableOf
import static org.grails.gorm.rx.transform.RxAstUtils.isObservableOfDomainClass
import static org.grails.gorm.rx.transform.RxAstUtils.isRxEntity

/**
 * Rx version of {@link FindByImplementer}
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
class FindOneByObservableImplementer extends FindOneByImplementer {

    @Override
    protected boolean isCompatibleReturnType(ClassNode domainClass, MethodNode methodNode, ClassNode returnType, String prefix) {
        return isObservableOfDomainClass(returnType)
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
            def ann = AstUtils.addAnnotationOrGetExisting(newMethodNode, RxSchedule)
            ann.setMember(RxScheduleIOTransformation.ANN_SINGLE_RESULT, ConstantExpression.TRUE)
            newMethodNode.addAnnotation(ann)
        }
        return super.buildReturnStatement(domainClass, abstractMethodNode, newMethodNode, queryExpression)
    }
}
