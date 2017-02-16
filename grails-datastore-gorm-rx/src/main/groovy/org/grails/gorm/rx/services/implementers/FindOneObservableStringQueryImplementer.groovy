package org.grails.gorm.rx.services.implementers

import grails.gorm.rx.services.RxSchedule
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.stmt.Statement
import org.grails.datastore.gorm.services.implementers.FindOneStringQueryImplementer

import static org.grails.datastore.mapping.reflect.AstUtils.addAnnotationOrGetExisting
import static org.grails.gorm.rx.transform.RxAstUtils.isObservableOfDomainClass
import static org.grails.gorm.rx.transform.RxAstUtils.isSingle

/**
 * Rx version of {@link FindOneStringQueryImplementer}
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
class FindOneObservableStringQueryImplementer extends FindOneStringQueryImplementer {
    @Override
    protected boolean isCompatibleReturnType(ClassNode domainClass, MethodNode methodNode, ClassNode returnType, String prefix) {
        return isObservableOfDomainClass(returnType)
    }

    @Override
    protected ClassNode resolveDomainClassFromSignature(ClassNode currentDomainClassNode, MethodNode methodNode) {
        methodNode.returnType.genericsTypes[0].type
    }

    @Override
    protected String getFindMethodToInvoke(ClassNode classNode, MethodNode newMethodNode) {
        if(isSingle(newMethodNode.returnType)) {
            return "find"
        }
        else {
            return "findAll"
        }
    }

    @Override
    protected Statement buildQueryReturnStatement(ClassNode domainClassNode, MethodNode abstractMethodNode, MethodNode newMethodNode, Expression args) {
        addAnnotationOrGetExisting(newMethodNode, RxSchedule)
        return super.buildQueryReturnStatement(domainClassNode, abstractMethodNode, newMethodNode, args)
    }
}
