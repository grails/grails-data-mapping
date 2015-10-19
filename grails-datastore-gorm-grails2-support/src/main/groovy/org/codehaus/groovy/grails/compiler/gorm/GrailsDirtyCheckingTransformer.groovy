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
package org.codehaus.groovy.grails.compiler.gorm

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.CompileUnit
import org.codehaus.groovy.classgen.GeneratorContext
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.compiler.injection.AstTransformer
import org.codehaus.groovy.grails.compiler.injection.GrailsArtefactClassInjector
import org.codehaus.groovy.grails.compiler.injection.GrailsDomainClassInjector
import org.grails.compiler.gorm.DirtyCheckingTransformer



/**
 * Dirty checking transformer integration for Grails 2.x
 *
 * @author Graeme Rocher
 * @since 5.0
 */
@AstTransformer
class GrailsDirtyCheckingTransformer extends DirtyCheckingTransformer implements GrailsDomainClassInjector, GrailsArtefactClassInjector{

    @Override
    void performInjection(SourceUnit source, GeneratorContext context, ClassNode classNode) {
        def compileUnit = context.compileUnit
        this.compilationUnit = new CompilationUnit(compileUnit.config, compileUnit.codeSource, compileUnit.classLoader)
        super.performInjection(source, context, classNode)
    }

    @Override
    String[] getArtefactTypes() {
        [DomainClassArtefactHandler.TYPE]
    }
}
