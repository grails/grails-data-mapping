package org.grails.orm.hibernate

import grails.persistence.Entity

import org.grails.orm.hibernate.cfg.MapFakeUserType
import org.junit.Test

class CustomUserTypeOverridesGrailsMapMappingTests extends AbstractGrailsHibernateTests {

    @Test
    void testUserTypeOverridesGrailsMapMappingTests() {
        def d = new DomainUserTypeMappings()

        d.myMap = [foo:"bar"]

        assert d.save(flush:true) != null

        // the map should not be mapped onto a join table but instead a single column
        session.connection().prepareStatement("select my_map from domain_user_type_mappings").execute()

        session.clear()

        d = DomainUserTypeMappings.get(d.id)

        assert d != null
    }

    @Override protected getDomainClasses() {
        [DomainUserTypeMappings]
    }
}

@Entity
class DomainUserTypeMappings {
    Long id
    Long version

    Map<String, String> myMap

    static mapping = {
        myMap type:MapFakeUserType
    }
}