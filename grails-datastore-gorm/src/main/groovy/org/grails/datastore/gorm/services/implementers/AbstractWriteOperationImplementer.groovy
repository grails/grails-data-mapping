package org.grails.datastore.gorm.services.implementers

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.grails.datastore.gorm.transactions.transform.TransactionalTransform

/**
 * Abstract implementer for write operations
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
abstract class AbstractWriteOperationImplementer extends AbstractServiceImplementer {
    /**
     * Subclasses should override to add the logic that implements the method
     *
     * @param targetClassNode The target class
     * @param abstractMethodNode The abstract method
     * @param newMethodNode The newly added method
     */
    abstract void doImplement(ClassNode domainClassNode, MethodNode abstractMethodNode, MethodNode newMethodNode, ClassNode targetClassNode)

    @Override
    final void implement(ClassNode domainClassNode, MethodNode abstractMethodNode, MethodNode newMethodNode, ClassNode targetClassNode) {
        // copy any annotations from the abstract method
        copyClassAnnotations(abstractMethodNode, newMethodNode)
        if(!TransactionalTransform.hasTransactionalAnnotation(newMethodNode)) {
            // read-only transaction by default
            newMethodNode.addAnnotation( new AnnotationNode(TransactionalTransform.MY_TYPE) )
        }

        doImplement(domainClassNode, abstractMethodNode, newMethodNode, targetClassNode)
        abstractMethodNode.putNodeMetaData(IMPLEMENTED, Boolean.TRUE)
    }
}
