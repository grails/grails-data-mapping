package org.grails.datastore.gorm.transform

import groovy.transform.CompilationUnitAware
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.AbstractASTTransformation

/**
 * Abstract base class for GORM AST transformations
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
abstract class AbstractGormASTTransformation extends AbstractASTTransformation implements CompilationUnitAware,ASTTransformation {

    CompilationUnit compilationUnit

    @Override
    void visit(ASTNode[] astNodes, SourceUnit source) {
        if (!(astNodes[0] instanceof AnnotationNode) || !(astNodes[1] instanceof AnnotatedNode)) {
            throw new RuntimeException("Internal error: wrong types: ${astNodes[0].getClass()} / ${astNodes[1].getClass()}");
        }

        AnnotatedNode classNode = (AnnotatedNode) astNodes[1];
        AnnotationNode annotationNode = (AnnotationNode) astNodes[0];


        if (!getAnnotationType().equals(annotationNode.getClassNode()) || !(classNode instanceof ClassNode)) {
            return;
        }

        Object appliedMarker = getAppliedMarker()
        if( classNode.getNodeMetaData(appliedMarker) == appliedMarker) {
            return
        }

        classNode.putNodeMetaData(appliedMarker, appliedMarker)

        visit( source, annotationNode, (ClassNode)classNode )
    }

    abstract void visit(SourceUnit source, AnnotationNode annotationNode, ClassNode classNode)

    protected abstract ClassNode getAnnotationType()

    protected abstract Object getAppliedMarker()

}
