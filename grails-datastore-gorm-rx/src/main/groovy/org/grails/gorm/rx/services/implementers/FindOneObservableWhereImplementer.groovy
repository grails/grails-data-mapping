package org.grails.gorm.rx.services.implementers

import grails.gorm.rx.services.RxSchedule
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.stmt.Statement
import org.grails.datastore.gorm.services.implementers.FindOneWhereImplementer

import static org.grails.datastore.mapping.reflect.AstUtils.addAnnotationOrGetExisting
import static org.grails.gorm.rx.transform.RxAstUtils.isObservableOfDomainClass
import static org.grails.gorm.rx.transform.RxAstUtils.isSingle

/**
 * Rx version of {@link FindOneWhereImplementer}
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
class FindOneObservableWhereImplementer extends FindOneWhereImplementer{

    @Override
    protected boolean isCompatibleReturnType(ClassNode domainClass, MethodNode newMethodNode, ClassNode returnType, String prefix) {
        return isObservableOfDomainClass(returnType)
    }

    @Override
    protected ClassNode resolveDomainClassFromSignature(ClassNode currentDomainClassNode, MethodNode methodNode) {
        methodNode.returnType.genericsTypes[0].type
    }

    @Override
    protected String getQueryMethodToExecute(ClassNode domainClass, MethodNode newMethodNode) {
        if(isSingle(newMethodNode.returnType)) {
            return "get"
        }
        else {
            return "list"
        }
    }

    @Override
    protected Statement buildReturnStatement(ClassNode domainClass, MethodNode abstractMethodNode, MethodNode methodNode, Expression queryExpression) {
        addAnnotationOrGetExisting(methodNode, RxSchedule)
        return super.buildReturnStatement(domainClass, abstractMethodNode, methodNode, queryExpression)
    }
}
