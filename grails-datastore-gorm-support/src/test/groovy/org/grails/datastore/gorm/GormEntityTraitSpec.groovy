package org.grails.datastore.gorm

import grails.artefact.Artefact
import grails.persistence.Entity
import spock.lang.Specification

/*
 * Copyright 2014 original authors
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

/**
 * @author graemerocher
 */
class GormEntityTraitSpec extends Specification {

    void "test that a class marked with @Artefact('Domain') is enhanced with GormEntityTraitSpec"() {
        expect:
        GormEntity.isAssignableFrom QueryMethodArtefactDomain
    }

    void "test that a class marked with @Entity is enhanced with GormEntityTraitSpec"() {
        expect:
        GormEntity.isAssignableFrom QueryMethodEntityDomain
    }

    void 'test that generic return values are respected'() {
        when:
        def method = QueryMethodArtefactDomain.methods.find { method ->
            def rt = method.getParameterTypes()
            rt && rt[0] == Closure && method.name == 'find'
        }

        then:
        method.returnType == QueryMethodArtefactDomain
    }
}

@Artefact('Domain')
class QueryMethodArtefactDomain {
    String name
}

@Entity
class QueryMethodEntityDomain {
    String name
}
