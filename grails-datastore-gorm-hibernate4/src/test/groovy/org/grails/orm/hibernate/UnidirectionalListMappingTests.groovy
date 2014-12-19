package org.grails.orm.hibernate

import org.junit.Test


class UnidirectionalListMappingTests extends AbstractGrailsHibernateTests {

    @Test
    void testUniListMapping() {

        def p = UnidirectionalListMappingPerson.newInstance()

        def e = UnidirectionalListMappingEmailAddress.newInstance()

        p.firstName = "Fred"
        p.lastName = "Flintstone"

        e.email = "fred@flintstones.com"
        p.addToEmailAddresses(e)

        p.save()

        session.flush()
        session.clear()

        assert p.id
        assert e.id

        def e2 = UnidirectionalListMappingEmailAddress.newInstance()
        e2.email = "foo@bar.com"
        e2.save()
        session.flush()

        assert e2.id
    }

    @Override
    protected getDomainClasses() {
        [UnidirectionalListMappingEmailAddress, UnidirectionalListMappingPerson]
    }
}
class UnidirectionalListMappingEmailAddress {
    Long id
    Long version
    String email
}

class UnidirectionalListMappingPerson {
    Long id
    Long version
    String firstName
    String lastName
    List emailAddresses
    static hasMany = [emailAddresses:UnidirectionalListMappingEmailAddress]
}
