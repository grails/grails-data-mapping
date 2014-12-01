package org.grails.datastore.gorm

import grails.artefact.Artefact
import grails.persistence.Entity
import spock.lang.Specification

class StaticQueryMethodsTraitSpec extends Specification {

    void "test that a class marked with @Artefact('Domain') is enhanced with StaticQueryMethods"() {
        expect:
        StaticQueryMethods.isAssignableFrom QueryMethodArtefactDomain
    }
    
    void "test that a class marked with @Entity is enhanced with StaticQueryMethods"() {
        expect:
        StaticQueryMethods.isAssignableFrom QueryMethodEntityDomain
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
