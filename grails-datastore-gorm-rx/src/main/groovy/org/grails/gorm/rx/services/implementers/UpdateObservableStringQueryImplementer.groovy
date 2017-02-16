package org.grails.gorm.rx.services.implementers

import grails.gorm.rx.services.RxSchedule
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.stmt.Statement
import org.grails.datastore.gorm.services.implementers.UpdateStringQueryImplementer

import static org.grails.datastore.mapping.reflect.AstUtils.addAnnotationOrGetExisting
import static org.grails.gorm.rx.transform.RxAstUtils.isObservableOf

/**
 * Rx version of {@link UpdateStringQueryImplementer}
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
class UpdateObservableStringQueryImplementer extends UpdateStringQueryImplementer {

    @Override
    protected boolean isCompatibleReturnType(ClassNode domainClass, MethodNode methodNode, ClassNode returnType, String prefix) {
        return isObservableOf(returnType, Number)
    }

    @Override
    protected ClassNode resolveDomainClassFromSignature(ClassNode currentDomainClassNode, MethodNode methodNode) {
        return currentDomainClassNode
    }

    @Override
    protected Statement buildQueryReturnStatement(ClassNode domainClassNode, MethodNode abstractMethodNode, MethodNode newMethodNode, Expression args) {
        addAnnotationOrGetExisting(newMethodNode, RxSchedule)
        return super.buildQueryReturnStatement(domainClassNode, abstractMethodNode, newMethodNode, args)
    }
}
