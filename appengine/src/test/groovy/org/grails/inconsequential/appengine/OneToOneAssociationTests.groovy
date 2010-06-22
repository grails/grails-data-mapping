package org.grails.inconsequential.appengine

import com.google.appengine.api.datastore.Key
import grails.persistence.Entity
import org.grails.inconsequential.appengine.testsupport.AppEngineDatastoreTestCase
import org.grails.inconsequential.core.ObjectDatastoreConnection
import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class OneToOneAssociationTests extends AppEngineDatastoreTestCase {
 @Test
  void testPersistOneToOneAssociation() {
    def ds = new AppEngineDatastore()
    ds.mappingContext.addPersistentEntity(Person)
    ObjectDatastoreConnection conn = ds.connect(null)


    def p = new Person(name:"Bob")
    p.address = new Address(number:"20", postCode:"39847")

    conn.persist(p)

    p = conn.retrieve(Person, new AppEngineKey(p.id))

    assert p != null
    assert "Bob" == p.name
    assert p.address != null
    assert "20" == p.address.number

  }
}
@Entity
class Person {
  Key id
  String name
  Address address
}
@Entity
class Address {
  Key id
  String number
  String postCode
}
