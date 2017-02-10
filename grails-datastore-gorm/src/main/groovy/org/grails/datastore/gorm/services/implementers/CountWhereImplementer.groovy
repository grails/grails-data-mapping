package org.grails.datastore.gorm.services.implementers

import grails.gorm.services.Where
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.grails.datastore.mapping.reflect.AstUtils

/**
 * Implements support for the {@link Where} annotation on {@link grails.gorm.services.Service} instances that return a multiple results
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
class CountWhereImplementer extends AbstractWhereImplementer {

    @Override
    boolean doesImplement(ClassNode domainClass, MethodNode methodNode) {
        if( AstUtils.findAnnotation(methodNode, Where) != null) {
            return isCompatibleReturnType(domainClass, methodNode, methodNode.returnType, methodNode.name)
        }
        return false
    }

    @Override
    protected boolean isCompatibleReturnType(ClassNode domainClass, MethodNode methodNode, ClassNode returnType, String prefix) {
        return AstUtils.isSubclassOfOrImplementsInterface(returnType, Number.name)
    }

    @Override
    protected String getQueryMethodToExecute() {
        return "count"
    }
}
