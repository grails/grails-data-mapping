package org.grails.datastore.mapping.reflect

import org.grails.datastore.mapping.model.config.GormProperties
import org.junit.Test

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


    static class Foo {
        static String name = "foo"

        String bar
    }
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