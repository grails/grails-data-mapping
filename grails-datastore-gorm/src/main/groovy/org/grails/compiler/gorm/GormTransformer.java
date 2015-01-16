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
package org.grails.compiler.gorm;

import grails.compiler.ast.AstTransformer;
import grails.compiler.ast.GrailsArtefactClassInjector;
import grails.persistence.Entity;
import groovy.transform.Canonical;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.SourceUnit;
import org.grails.compiler.injection.GrailsASTUtils;
import org.grails.core.artefact.DomainClassArtefactHandler;
import org.grails.io.support.GrailsResourceUtils;

import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Transforms GORM entities making the GORM API available to Java.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
@AstTransformer
public class GormTransformer implements GrailsArtefactClassInjector {

    public static final int PUBLIC_STATIC_MODIFIER = Modifier.PUBLIC | Modifier.STATIC;

    public static final String NEW_INSTANCE_METHOD = "newInstance";

    public static final String CREATE_METHOD_NAME = "create";
    private static final Set<String> TRANSFORMED_CLASSES = new HashSet<String>();

    @Override
    public String[] getArtefactTypes() {
        return new String[] {DomainClassArtefactHandler.TYPE};
    }

    public boolean shouldInject(URL url) {
        return GrailsResourceUtils.isDomainClass(url);
    }

    public static Collection<String> getKnownEntityNames() {
        return Collections.unmodifiableCollection( TRANSFORMED_CLASSES );
    }

    @Override
    public void performInjection(SourceUnit source, GeneratorContext context, ClassNode classNode) {
        performInjectionOnAnnotatedClass(source, classNode);
    }

    @Override
    public void performInjection(SourceUnit source, ClassNode classNode) {
        if(GrailsASTUtils.hasAnnotation(classNode, Canonical.class)) {
            GrailsASTUtils.error(source, classNode, "Class [" + classNode.getName() + "] is marked with @groovy.transform.Canonical which is not supported for GORM entities.", true);
        }
        classNode.setUsingGenerics(true);
        GrailsASTUtils.addAnnotationIfNecessary(classNode, Entity.class);

        TRANSFORMED_CLASSES.add(classNode.getName());

        final BlockStatement methodBody = new BlockStatement();
        methodBody.addStatement(new ExpressionStatement(new MethodCallExpression(new ClassExpression(classNode), NEW_INSTANCE_METHOD,ZERO_ARGS)));
        MethodNode methodNode = classNode.getDeclaredMethod(CREATE_METHOD_NAME, ZERO_PARAMETERS);
        classNode = GrailsASTUtils.nonGeneric(classNode);
        if (methodNode == null) {
            classNode.addMethod(new MethodNode(CREATE_METHOD_NAME, PUBLIC_STATIC_MODIFIER, classNode, ZERO_PARAMETERS,null, methodBody));
        }
    }

    @Override
    public void performInjectionOnAnnotatedClass(SourceUnit source, ClassNode classNode) {
        performInjection(source, classNode);
    }
}
