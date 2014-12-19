package org.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test

class CustomComponentMappingTests extends  AbstractGrailsHibernateTests {

    // Related to GRAILS-5447
    @Test
    void testCustomEmbeddedComponentMapping() {

        def p = new CustomComponentMappingParent(component:new CustomComponentMappingComponent(property:10))

        assert p.save(flush:true) != null

        session.clear()

        p = CustomComponentMappingParent.get(p.id)

        assert p.component != null
        assert p.component.property == 10

        session.connection().prepareStatement("select prop from custom_component_mapping_parent").execute()
    }

    @Override
    protected getDomainClasses() {
        [CustomComponentMappingParent]
    }
}

@Entity
class CustomComponentMappingParent {
    Long id
    Long version

    CustomComponentMappingComponent component
    static embedded = ["component"]
}

//@Entity
class CustomComponentMappingComponent {
    BigInteger property
    static mapping = {
        property(column: "prop")
    }
}
