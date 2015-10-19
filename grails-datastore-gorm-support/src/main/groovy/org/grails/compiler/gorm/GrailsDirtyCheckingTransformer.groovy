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
package org.grails.compiler.gorm

import grails.compiler.ast.GrailsArtefactClassInjector
import grails.compiler.ast.GrailsDomainClassInjector
import org.codehaus.groovy.grails.compiler.injection.AstTransformer
import org.grails.core.artefact.DomainClassArtefactHandler


/**
 * Concrete implementation of DirtyCheckTransformer for Grails 3
 *
 * @author Graeme Rocher
 * @since 5.0
 */
@AstTransformer
class GrailsDirtyCheckingTransformer extends DirtyCheckingTransformer implements GrailsDomainClassInjector, GrailsArtefactClassInjector{

    @Override
    String[] getArtefactTypes() {
        [DomainClassArtefactHandler.TYPE]
    }
}
