package org.grails.orm.hibernate


import static junit.framework.Assert.*
import org.junit.Test

class ComponentDomainTests extends AbstractGrailsHibernateTests {

    @Test
    void testComponentDomain() {
        def personClass = ga.getDomainClass(ComponentDomainPerson.name)

        def homeAddress = personClass.getPropertyByName("homeAddress")
        def workAddress = personClass.getPropertyByName("workAddress")
        assertTrue homeAddress.isEmbedded()
        assertTrue workAddress.isEmbedded()

        assertNotNull homeAddress.referencedDomainClass
        assertEquals "ComponentDomainAddress",homeAddress.referencedDomainClass.name
    }

    @Override
    protected getDomainClasses() {
        [ComponentDomainPerson, ComponentDomainAddress]
    }
}

class ComponentDomainPerson {
    Long id
    Long version
    String name
    ComponentDomainAddress homeAddress
    ComponentDomainAddress workAddress

    static embedded = ['homeAddress', 'workAddress']
}
class ComponentDomainAddress {
    Long id
    Long version
    String number
    String postCode
}
