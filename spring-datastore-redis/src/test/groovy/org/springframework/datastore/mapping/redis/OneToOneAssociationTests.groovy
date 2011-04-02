package org.springframework.datastore.mapping.redis

import grails.persistence.Entity

import org.junit.Test
import org.springframework.datastore.mapping.core.Session

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class OneToOneAssociationTests extends AbstractRedisTest {

    @Test
    void testPersistOneToOneAssociation() {
        ds.mappingContext.addPersistentEntity(Person)
        Session conn = ds.connect(null)

        def p = new Person(name:"Bob")
        p.address = new Address(number:"20", postCode:"39847")

        conn.persist(p)

        p = conn.retrieve(Person, p.id)

        assert p != null
        assert "Bob" == p.name
        assert p.address != null
        assert "20" == p.address.number
    }
}

@Entity
class Person {
    Long id
    String name
    Address address
}

@Entity
class Address {
    Long id
    String number
    String postCode
}
