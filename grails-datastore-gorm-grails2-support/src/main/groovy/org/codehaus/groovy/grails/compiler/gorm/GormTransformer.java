/*
 * Copyright 2015 original authors
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


/**
 * GormTransformer for Grails 2.x
 *
 * @author Graeme Rocher
 * @since 5.0
 */

import groovy.transform.Canonical;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.CompileUnit;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.compiler.injection.AstTransformer;
import org.codehaus.groovy.grails.compiler.injection.GrailsASTUtils;
import org.codehaus.groovy.grails.compiler.injection.GrailsArtefactClassInjector;
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils;
import org.grails.compiler.gorm.GormEntityTransformation;
import org.grails.datastore.mapping.reflect.AstUtils;

import java.net.URL;
import java.util.Collection;

@AstTransformer
public class GormTransformer implements GrailsArtefactClassInjector {

    @Override
    public String[] getArtefactTypes() {
        return new String[] {DomainClassArtefactHandler.TYPE};
    }

    public boolean shouldInject(URL url) {
        return GrailsResourceUtils.isDomainClass(url);
    }

    public static Collection<String> getKnownEntityNames() {
        return AstUtils.getKnownEntityNames();
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
        new GormEntityTransformation().visit(classNode, source);
        final CompileUnit compileUnit = source.getAST().getUnit();
        org.codehaus.groovy.transform.trait.TraitComposer.doExtendTraits(classNode, source, new CompilationUnit(compileUnit.getConfig(), compileUnit.getCodeSource(), compileUnit.getClassLoader()));
    }



    @Override
    public void performInjectionOnAnnotatedClass(SourceUnit source, ClassNode classNode) {
        performInjection(source, classNode);
    }
}
