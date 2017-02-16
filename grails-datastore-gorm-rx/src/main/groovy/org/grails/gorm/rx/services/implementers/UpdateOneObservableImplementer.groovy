package org.grails.gorm.rx.services.implementers

import grails.gorm.rx.services.RxSchedule
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.grails.datastore.gorm.services.implementers.UpdateOneImplementer
import org.grails.gorm.rx.transform.RxScheduleIOTransformation

import static org.grails.datastore.mapping.reflect.AstUtils.addAnnotationOrGetExisting
import static org.grails.gorm.rx.transform.RxAstUtils.isObservableOfDomainClass
import static org.grails.gorm.rx.transform.RxAstUtils.isRxEntity

/**
 * Rx version of {@link UpdateOneImplementer}
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
class UpdateOneObservableImplementer extends UpdateOneImplementer {

    @Override
    protected boolean isCompatibleReturnType(ClassNode domainClass, MethodNode methodNode, ClassNode returnType, String prefix) {
        return isObservableOfDomainClass(returnType)
    }

    @Override
    void doImplement(ClassNode domainClassNode, MethodNode abstractMethodNode, MethodNode newMethodNode, ClassNode targetClassNode) {
        if(!isRxEntity(domainClassNode)) {
            def ann = addAnnotationOrGetExisting(newMethodNode, RxSchedule)
            ann.setMember(RxScheduleIOTransformation.ANN_SINGLE_RESULT, ConstantExpression.TRUE)
            newMethodNode.addAnnotation(ann)
        }

        super.doImplement(domainClassNode, abstractMethodNode, newMethodNode, targetClassNode)
    }
}
