package org.grails.datastore.gorm.validation.javax.implementers

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.grails.datastore.gorm.services.ServiceEnhancer
import org.grails.datastore.gorm.validation.javax.JavaxValidatorRegistry
import org.grails.datastore.mapping.reflect.AstUtils

import javax.validation.Constraint

/**
 * Adds method parameter validation to {@link grails.gorm.services.Service} instances
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
class MethodValidationImplementer implements ServiceEnhancer {
    @Override
    boolean doesImplement(ClassNode domainClass, MethodNode methodNode) {
        return false
    }

    @Override
    void implement(ClassNode domainClassNode, MethodNode abstractMethodNode, MethodNode newMethodNode, ClassNode targetClassNode) {
        // no-op
    }

    @Override
    boolean doesEnhance(ClassNode domainClass, MethodNode methodNode) {
        if(JavaxValidatorRegistry.isAvailable()) {
            return methodNode.annotations.any() { AnnotationNode ann -> AstUtils.findAnnotation(ann.classNode, Constraint) != null }
        }
        return false
    }

    @Override
    void enhance(ClassNode domainClassNode, MethodNode abstractMethodNode, MethodNode newMethodNode, ClassNode targetClassNode) {
        BlockStatement body = (BlockStatement)newMethodNode.code
        // TODO: implement javax.validation enhancement
    }
}
