package org.grails.inconsequential.redis

import org.grails.inconsequential.core.ObjectDatastoreConnection
import org.junit.Test
import grails.persistence.Entity

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class OneToOneAssociationTests {
  @Test
  void testPersistOneToOneAssociation() {
    def ds = new RedisDatastore()
    ds.mappingContext.addPersistentEntity(Person)
    ObjectDatastoreConnection conn = ds.connect(null)


    def p = new Person(name:"Bob")
    p.address = new Address(number:"20", postCode:"39847")

    conn.persist(p)

    p = conn.retrieve(Person, new RedisKey(p.id))

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


