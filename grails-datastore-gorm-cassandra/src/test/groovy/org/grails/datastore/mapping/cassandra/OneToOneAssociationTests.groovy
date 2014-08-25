package org.grails.datastore.mapping.cassandra

import grails.persistence.Entity

import org.grails.datastore.mapping.core.Session
import org.junit.Ignore
import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.1
 */
@Ignore
class OneToOneAssociationTests extends AbstractCassandraTest {

    @Test
    void testPersistOneToOneAssociation() {
    
        def ds = new CassandraDatastore()
        ds.mappingContext.addPersistentEntity(Person)
        Session conn = ds.connect()

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
    UUID id
    String name
    Address address
}

@Entity
class Address {
    UUID id
    String number
    String postCode
}
