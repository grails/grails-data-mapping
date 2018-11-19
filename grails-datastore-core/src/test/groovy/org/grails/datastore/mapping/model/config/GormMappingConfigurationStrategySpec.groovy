package org.grails.datastore.mapping.model.config

import org.grails.datastore.mapping.keyvalue.mapping.config.GormKeyValueMappingFactory
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher
import spock.lang.Specification

class GormMappingConfigurationStrategySpec extends Specification {

    void "test getAssociationMap subclass overrides parent"() {
        ClassPropertyFetcher cpf = ClassPropertyFetcher.forClass(B)
        def strategy = new GormMappingConfigurationStrategy(new GormKeyValueMappingFactory("test"))

        when:
        Map associations = strategy.getAssociationMap(cpf)

        then:
        associations.size() == 1
        associations.get("foo") == Integer
    }

    class A {
        static hasMany = [foo: String]
    }
    class B extends A {
        static hasMany = [foo: Integer]
    }
}
