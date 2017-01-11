package org.grails.datastore.gorm.services

import grails.gorm.services.Service
import groovy.transform.CompilationUnitAware
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.grails.datastore.gorm.transform.AbstractTraitApplyingGormASTTransformation
import org.grails.datastore.mapping.reflect.AstUtils

/**
 * Makes a class implement the {@link org.grails.datastore.mapping.services.Service} trait and generates the necessary
 * service loader META-INF/services file.
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class ServiceTransformation extends AbstractTraitApplyingGormASTTransformation implements CompilationUnitAware,ASTTransformation {

    private static final ClassNode MY_TYPE = new ClassNode(Service.class);
    private static final Object APPLIED_MARKER  = new Object()

    @Override
    protected Class getTraitClass() {
        org.grails.datastore.mapping.services.Service
    }

    @Override
    protected ClassNode getAnnotationType() {
        return MY_TYPE
    }

    @Override
    protected Object getAppliedMarker() {
        return APPLIED_MARKER
    }

    @Override
    void visitAfterTraitApplied(SourceUnit sourceUnit, AnnotationNode annotationNode, ClassNode classNode) {
        File targetDirectory = sourceUnit.configuration.targetDirectory
        if(targetDirectory == null) {
            targetDirectory = new File("build/resources/main")
        }

        File servicesDir = new File(targetDirectory, "META-INF/services")
        servicesDir.mkdirs()

        try {
            new File(servicesDir, "/$org.grails.datastore.mapping.services.Service.name").text = classNode.name
        } catch (Throwable e) {
            AstUtils.warning(sourceUnit, classNode, "Error generating service loader descriptor for class [$classNode.name]: $e.message")
        }
    }
}
