/*
 * Copyright 2011 SpringSource
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
package org.codehaus.groovy.grails.compiler.gorm;

import grails.persistence.Entity;
import grails.persistence.PersistenceMethod;

import groovy.transform.Canonical;

import java.net.URL;
import java.util.*;

import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.metaclass.CreateDynamicMethod;
import org.codehaus.groovy.grails.compiler.injection.AbstractGrailsArtefactTransformer;
import org.codehaus.groovy.grails.compiler.injection.AstTransformer;
import org.codehaus.groovy.grails.compiler.injection.GrailsASTUtils;
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils;
import org.grails.datastore.gorm.GormInstanceApi;
import org.grails.datastore.gorm.GormStaticApi;

/**
 * Transforms GORM entities making the GORM API available to Java.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
@AstTransformer
public class GormTransformer extends AbstractGrailsArtefactTransformer {

    public static final String NEW_INSTANCE_METHOD = "newInstance";

    private static final Set<String> EXCLUDES = new HashSet<String>(Arrays.asList("create", "setTransactionManager"));
    private static final Set<String> INCLUDES = new HashSet<String>(Arrays.asList("getAll", "getCount", "getValidationSkipMap", "getValidationErrorsMap", "getAsync"));
    private static final Set<String> TRANSFORMED_CLASSES = new HashSet<String>();

    @Override
    protected boolean isStaticMethodExcluded(ClassNode classNode, MethodNode declaredMethod) {
        return EXCLUDES.contains(declaredMethod.getName());
    }

    @Override
    protected boolean isStaticMethodIncluded(ClassNode classNode, MethodNode declaredMethod) {
        return INCLUDES.contains(declaredMethod.getName());
    }

    @Override
    public String getArtefactType() {
        return DomainClassArtefactHandler.TYPE;
    }

    public Class<?> getInstanceImplementation() {
        return GormInstanceApi.class;
    }

    public Class<?> getStaticImplementation() {
        return GormStaticApi.class;
    }

    @Override
    protected boolean requiresStaticLookupMethod() {
        return true;
    }

    @Override
    protected AnnotationNode getMarkerAnnotation() {
        return new AnnotationNode(new ClassNode(PersistenceMethod.class).getPlainNodeReference());
    }

    @Override
    protected void performInjectionInternal(String apiInstanceProperty, SourceUnit source, ClassNode classNode) {
        if(GrailsASTUtils.hasAnnotation(classNode, Canonical.class)) {
            GrailsASTUtils.error(source, classNode, "Class [" + classNode.getName() + "] is marked with @groovy.transform.Canonical which is not supported for GORM entities.", true);
        }
        classNode.setUsingGenerics(true);
        GrailsASTUtils.addAnnotationIfNecessary(classNode, Entity.class);

        TRANSFORMED_CLASSES.add(classNode.getName());

        final BlockStatement methodBody = new BlockStatement();
        methodBody.addStatement(new ExpressionStatement(new MethodCallExpression(new ClassExpression(classNode), NEW_INSTANCE_METHOD,ZERO_ARGS)));
        MethodNode methodNode = classNode.getDeclaredMethod(CreateDynamicMethod.METHOD_NAME, ZERO_PARAMETERS);
        classNode = GrailsASTUtils.nonGeneric(classNode);
        if (methodNode == null) {
            classNode.addMethod(new MethodNode(CreateDynamicMethod.METHOD_NAME, PUBLIC_STATIC_MODIFIER, classNode, ZERO_PARAMETERS,null, methodBody));
        }
    }

    public boolean shouldInject(URL url) {
        return GrailsResourceUtils.isDomainClass(url);
    }

    public static Collection<String> getKnownEntityNames() {
        return Collections.unmodifiableCollection( TRANSFORMED_CLASSES );
    }
}
