/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.datastore.gorm.services

import grails.gorm.services.Service
import groovy.transform.CompilationUnitAware
import groovy.transform.CompileStatic
import groovy.transform.Memoized
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.control.io.FileReaderSource
import org.codehaus.groovy.control.io.ReaderSource
import org.codehaus.groovy.control.io.StringReaderSource
import org.codehaus.groovy.control.io.URLReaderSource
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.grails.datastore.gorm.services.implementers.DeleteImplementer
import org.grails.datastore.gorm.services.implementers.FindAndDeleteImplementer
import org.grails.datastore.gorm.services.implementers.FindAllImplementer
import org.grails.datastore.gorm.services.implementers.FindByImplementer
import org.grails.datastore.gorm.services.implementers.FindOneByImplementer
import org.grails.datastore.gorm.services.implementers.FindOneImplementer
import org.grails.datastore.gorm.services.implementers.SaveImplementer
import org.grails.datastore.gorm.transform.AbstractTraitApplyingGormASTTransformation
import org.grails.datastore.mapping.core.order.OrderedComparator

import static org.grails.datastore.mapping.reflect.AstUtils.*

/**
 * Makes a class implement the {@link org.grails.datastore.mapping.services.Service} trait and generates the necessary
 * service loader META-INF/services file.
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class ServiceTransformation extends AbstractTraitApplyingGormASTTransformation implements CompilationUnitAware,ASTTransformation {

    private static final ClassNode MY_TYPE = new ClassNode(Service.class);
    private static final Object APPLIED_MARKER  = new Object()
    private static final List<ServiceImplementer> DEFAULT_IMPLEMENTORS = [
            new FindAllImplementer(),
            new FindOneImplementer(),
            new FindByImplementer(),
            new FindOneByImplementer(),
            new FindAndDeleteImplementer(),
            new DeleteImplementer(),
            new SaveImplementer()] as List<ServiceImplementer>

    private static Iterable<ServiceImplementer> LOADED_IMPLEMENTORS = null
    public static final String NO_IMPLEMENTATIONS_MESSAGE = "No implementations possible for method. Please use an abstract class instead and provide an implementation."

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
        // if the class node is an interface we are going to try and generate an implementation
        // and add the implementation as an inner class. If any method of the interface cannot be implemented
        // a compilation error occurs
        if(classNode.isInterface()) {
            // create a new class to represent the implementation
            String packageName = classNode.packageName ? "${classNode.packageName}." : ""
            ClassNode impl = new ClassNode("${packageName}\$${classNode.nameWithoutPackage}Implementation", // class name
                                            ACC_PUBLIC, // public
                                            ClassHelper.OBJECT_TYPE,
                                            [classNode.plainNodeReference] as ClassNode[])
            // add compile static by default
            impl.addAnnotation(new AnnotationNode(COMPILE_STATIC_TYPE))
            // weave the trait class
            ClassExpression ce = (ClassExpression)annotationNode.getMember("value")
            ClassNode targetDomainClass = ce != null ? ce.type : ClassHelper.OBJECT_TYPE
            // weave with generic argument
            weaveTrait(impl, sourceUnit, getTraitClass(), targetDomainClass)

            List<MethodNode> methods = classNode.getMethods()
            Iterable<ServiceImplementer> implementors = findServiceImplementors()

            for(MethodNode method in methods) {
                MethodNode methodImpl = null

                for(ServiceImplementer implementer in implementors) {
                    if(implementer.doesImplement(targetDomainClass, method)) {
                        if(methodImpl == null) {

                            methodImpl = new MethodNode(
                                    method.name,
                                    ACC_PUBLIC,
                                    method.returnType.plainNodeReference,
                                    copyParameters(method.parameters),
                                    method.exceptions,
                                    new BlockStatement())
                            impl.addMethod(methodImpl)
                        }

                        implementer.implement(targetDomainClass, method, methodImpl, impl)
                    }
                    else if(implementer instanceof ServiceEnhancer) {
                        ServiceEnhancer enhancer = ((ServiceEnhancer)implementer)
                        if(enhancer.doesEnhance(targetDomainClass, method)) {
                            enhancer.enhance(targetDomainClass, method, methodImpl, impl)
                        }
                    }
                }

                if(methodImpl == null) {
                    addError(NO_IMPLEMENTATIONS_MESSAGE, method)
                    break
                }
            }

            Expression exposeExpr = annotationNode.getMember("expose")
            if( exposeExpr == null || (exposeExpr instanceof ConstantExpression && exposeExpr == ConstantExpression.TRUE) ) {
                generateServiceDescriptor(sourceUnit, impl)
            }

            sourceUnit.getAST().addClass(impl)
        }
        else {
            Expression exposeExpr = annotationNode.getMember("expose")
            if( exposeExpr == null || (exposeExpr instanceof ConstantExpression && exposeExpr == ConstantExpression.TRUE) ) {
                generateServiceDescriptor(sourceUnit, classNode)
            }
        }
    }

    @Memoized
    protected Iterable<ServiceImplementer> findServiceImplementors() {
        if(LOADED_IMPLEMENTORS == null) {
            Iterable<ServiceImplementer> implementors = ServiceLoader.load(ServiceImplementer, getClass().classLoader)
            if (!implementors.iterator().hasNext()) {
                implementors = ServiceLoader.load(ServiceImplementer, Thread.currentThread().contextClassLoader)
            }
            implementors = (implementors + DEFAULT_IMPLEMENTORS).unique { ServiceImplementer o1 ->
                 o1.class.name
            }
            LOADED_IMPLEMENTORS = implementors.sort(true, new OrderedComparator<ServiceImplementer>())
        }
        return LOADED_IMPLEMENTORS
    }

    protected void generateServiceDescriptor(SourceUnit sourceUnit, ClassNode classNode) {
        ReaderSource readerSource = sourceUnit.getSource()
        // Don't generate for runtime compiled scripts
        if(readerSource instanceof FileReaderSource || readerSource instanceof URLReaderSource) {

            File targetDirectory = sourceUnit.configuration.targetDirectory
            if (targetDirectory == null) {
                targetDirectory = new File("build/resources/main")
            }

            File servicesDir = new File(targetDirectory, "META-INF/services")
            servicesDir.mkdirs()

            String className = classNode.name
            try {
                def descriptor = new File(servicesDir, org.grails.datastore.mapping.services.Service.name)
                if (descriptor.exists()) {
                    String ls = System.getProperty('line.separator')
                    String contents = descriptor.text
                    def entries = contents.split('\\n')
                    if (!entries.contains(className)) {
                        descriptor.append("${ls}${className}")
                    }
                } else {
                    descriptor.text = className
                }
            } catch (Throwable e) {
                warning(sourceUnit, classNode, "Error generating service loader descriptor for class [${className}]: $e.message")
            }
        }
    }
}
