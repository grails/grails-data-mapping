package org.grails.datastore.gorm.schemaless

import groovy.transform.Generated
import spock.lang.Issue
import spock.lang.Specification

import java.lang.reflect.Method


class DynamicDomainSpec extends Specification {

    @Issue("GDM-769")
    void "Test a domain with dynamic attributes doesn't try to set readonly properties"() {
        given:
        DynamicEntity entity = new DynamicEntity()

        when:
        entity.putAt("foo", 123)
        entity.putAt("name", "Sally")

        then:
        entity.foo == "foo"
        entity.name == "Sally"
        entity.getAt("foo") == "foo"
        !entity.attributes().containsKey("name")
        entity.attributes().foo == 123
    }

    void "test that all DynamicAttributes trait methods are marked as Generated"() {
        expect: "all DynamicAttributes methods are marked as Generated on implementation class"
        DynamicAttributes.getMethods().each { Method traitMethod ->
            assert DynamicEntity.class.getMethod(traitMethod.name, traitMethod.parameterTypes).isAnnotationPresent(Generated)
        }
    }
}

class DynamicEntity implements DynamicAttributes {

    String name

    String getFoo() {
        "foo"
    }

}