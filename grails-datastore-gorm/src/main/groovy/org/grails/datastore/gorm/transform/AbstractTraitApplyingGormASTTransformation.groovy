package org.grails.datastore.gorm.transform

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.control.SourceUnit

import static org.grails.datastore.mapping.reflect.AstUtils.OBJECT_CLASS_NODE

/**
 * An abstract transformation that applies a Trait
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
abstract class AbstractTraitApplyingGormASTTransformation extends AbstractGormASTTransformation {
    @Override
    void visit(SourceUnit source, AnnotationNode annotationNode, ClassNode classNode) {
        ClassNode traitClassNode = ClassHelper.make(getTraitClass()).getPlainNodeReference()
        ClassNode superClass = classNode.getSuperClass()
        final shouldWeave = superClass.equals(OBJECT_CLASS_NODE)

        if(!shouldWeave) {
            shouldWeave = !classNode.implementsInterface(traitClassNode)
        }

        if(shouldWeave ) {
            classNode.addInterface(traitClassNode)
            if(compilationUnit != null) {
                org.codehaus.groovy.transform.trait.TraitComposer.doExtendTraits(classNode, source, compilationUnit);
            }
        }

        visitAfterTraitApplied(source, annotationNode, classNode)
    }

    void visitAfterTraitApplied(SourceUnit sourceUnit, AnnotationNode annotationNode, ClassNode classNode) {
        // no-dop
    }

    protected abstract Class getTraitClass()

}
