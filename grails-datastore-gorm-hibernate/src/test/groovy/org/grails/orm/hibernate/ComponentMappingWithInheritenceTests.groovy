package org.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test

import static junit.framework.Assert.*

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Jan 22, 2009
 */
class ComponentMappingWithInheritenceTests extends AbstractGrailsHibernateTests {


    // test for GRAILS-1217
    @Test
    void testEmbeddedColumnsNullableWithTablePerHeirarchyInheritance() {

        def newuser = new ComponentMappingWithInheritenceSiteUser(login:'base@site.com', password:'aasa132',nick:'tester')

        // this simply tests that it is possibly to save a super class in table-per-heirarchy inheritance
        // with table-per-heirarchy inheritance the embedded items columns should be nullable
        assertNotNull "Validation should have succeeded",newuser.save(flush:true)
    }

    @Override
    protected getDomainClasses() {
        [ComponentMappingWithInheritenceSiteUser, ComponentMappingWithInheritenceCustomer]
    }
}
@Entity
class ComponentMappingWithInheritenceSiteUser {
    Long id
    Long version
    String login
    String password
    String nick
    String status = "active"
    Date signUpDate = new Date()
}

@Entity
class ComponentMappingWithInheritenceCustomer extends ComponentMappingWithInheritenceSiteUser {
    String firstName
    String lastName
    String telephone
    ComponentMappingWithInheritenceAddress address

    static embedded = ['address']
}

class ComponentMappingWithInheritenceAddress {
    String street
    String zipcode
    String country
}

