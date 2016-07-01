package org.grails.compiler.gorm

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.GenericsType
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.ModuleNode
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.grails.datastore.gorm.query.transform.DetachedCriteriaTransformer
import org.grails.datastore.mapping.reflect.AstUtils

/**
 * Repairs the AST due to bugs in the way {@link org.codehaus.groovy.transform.trait.TraitComposer} works. See https://issues.apache.org/jira/browse/GROOVY-7846
 *
 * Once those issues are addressed this can be removed
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
@GroovyASTTransformation(phase= CompilePhase.CANONICALIZATION)
class GlobalTraitRepairTransformation implements ASTTransformation {

    private static final Object TRANSFORM_APPLIED_MARKER = new Object()

    private static final boolean ENABLED

    static {
        String groovyVersion = GroovySystem.version
        if(groovyVersion.startsWith("2.4.")) {
            try {
                ENABLED = groovyVersion.split(/\./)[2].toInteger() < 7
            } catch (Throwable e) {
                ENABLED = false
            }
        }
        else {
            ENABLED = false
        }
    }
    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        ModuleNode ast = source.getAST();
        List<ClassNode> classes = ast.getClasses();
        for (ClassNode aClass : classes) {
            visitClass(aClass)
        }
    }

    void visitClass(ClassNode aClass) {
        if (aClass.getNodeMetaData(TRANSFORM_APPLIED_MARKER) == null) {

            if (AstUtils.implementsInterface(aClass, "org.grails.datastore.gorm.GormEntity") || AstUtils.implementsInterface(aClass, "grails.gorm.rx.RxEntity")) {
                aClass.putNodeMetaData(TRANSFORM_APPLIED_MARKER, Boolean.TRUE)
                def allMethods = aClass.getMethods()
                for (MethodNode mn in allMethods) {
                    for (GenericsType gt in mn.returnType.genericsTypes) {
                        if (gt.name == aClass.name) {
                            gt.setType(ClassHelper.make(aClass.name).getPlainNodeReference())
                        } else if (gt.name == 'T') {
                            mn.setReturnType(ClassHelper.make(Object).getPlainNodeReference())
                        }
                    }
                }
            }
        }
    }
}