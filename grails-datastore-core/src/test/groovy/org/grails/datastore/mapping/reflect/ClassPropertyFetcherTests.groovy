package org.grails.datastore.mapping.reflect

import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValueMappingFactory
import org.grails.datastore.mapping.model.config.GormProperties
import org.junit.Test

import java.beans.PropertyDescriptor

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class ClassPropertyFetcherTests  {

    @Test
    void testGetProperty() {
        def cpf = ClassPropertyFetcher.forClass(Foo)

        assert 'foo' == cpf.getPropertyValue("name")
        assert cpf.getPropertiesAssignableToType(CharSequence).size() == 1
        assert cpf.getPropertiesAssignableToType(String).size() == 1
    }

    @Test
    void testGetStaticPropertyInheritance() {
        def tc = ClassPropertyFetcher.forClass(TransientChild)
        def tp = ClassPropertyFetcher.forClass(TransientParent)
        def tsc = ClassPropertyFetcher.forClass(TransientSubChild)

        assert tp.getStaticPropertyValuesFromInheritanceHierarchy(GormProperties.TRANSIENT, Collection) == [[]]
        assert tc.getStaticPropertyValuesFromInheritanceHierarchy(GormProperties.TRANSIENT, Collection) == [[], ["transientProperty"]]
        assert tsc.getStaticPropertyValuesFromInheritanceHierarchy(GormProperties.TRANSIENT, Collection) == [[], ["transientProperty"], [], ["bar"]]
    }


    @Test
    void testClassPropertyFetcherWithTraitProperty() {
        def cpf = ClassPropertyFetcher.forClass(DomainWithTrait)

        def metaProperties = cpf.getMetaProperties()

        assert DomainWithTrait.getDeclaredMethod("getFrom").returnType == DomainWithTrait
        assert metaProperties.size() == 2

        def prop = metaProperties.find { it.name == 'from' }

        assert prop != null
        assert prop.type == DomainWithTrait

        KeyValueMappingFactory mappingFactory = new KeyValueMappingFactory("test")

        PropertyDescriptor descriptor = mappingFactory.createPropertyDescriptor(DomainWithTrait, prop)
        assert descriptor != null
    }

    static class Foo {
        static String name = "foo"

        String bar
    }
}

trait TestTrait<F extends Serializable> {
    F from
}

class DomainWithTrait implements Serializable, TestTrait<DomainWithTrait> {
    String name
}

class TransientParent {

    static mapWith = 'neo4j'
    static transients = []
}

class TransientChild extends TransientParent {
    String name
    String transientProperty

    String getTransientProperty() {
        return transientProperty
    }

    void setTransientProperty(String transientProperty) {
        this.transientProperty = transientProperty
    }
    static transients = ["transientProperty"]
}

class TransientSubChild extends TransientChild {

    String foo
    String bar

    static transients = ["bar"]
}