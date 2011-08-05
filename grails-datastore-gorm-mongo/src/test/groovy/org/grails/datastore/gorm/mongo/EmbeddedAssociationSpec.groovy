package org.grails.datastore.gorm.mongo

import grails.gorm.tests.GormDatastoreSpec
import grails.persistence.Entity

class EmbeddedAssociationSpec extends GormDatastoreSpec {

    static {
        GormDatastoreSpec.TEST_CLASSES << Individual << Individual2 << Address
    }

    void "Test persistence of embedded entities"() {
        given:
            def i = new Individual(name:"Bob", address: new Address(postCode:"30483"))

            i.save(flush:true)
            session.clear()

        when:
            i = Individual.findByName("Bob")

        then:
            i != null
            i.name == 'Bob'
            i.address != null
            i.address.postCode == '30483'
    }

    void "Test persistence of embedded entity collections"() {
        given:
            def i = new Individual2(name:"Bob", address: new Address(postCode:"30483"))
            i.otherAddresses = [new Address(postCode: "12345"), new Address(postCode: "23456")]
            i.save(flush:true)
            session.clear()

        when:
            i = Individual2.findByName("Bob")

        then:
            i != null
            i.name == 'Bob'
            i.address != null
            i.address.postCode == '30483'
            i.otherAddresses != null
            i.otherAddresses.size() == 2
            i.otherAddresses[0].postCode == '12345'
            i.otherAddresses[1].postCode == '23456'
    }
}

@Entity
class Individual {
    Long id
    String name
    Address address
    static embedded = ['address']

    static mapping = {
        name index:true
    }
}

@Entity
class Individual2 {
    Long id
    String name
    Address address
    List<Address> otherAddresses
    static embedded = ['address', 'otherAddresses']

    static mapping = {
        name index:true
    }
}

@Entity
class Address {
    Long id
    String postCode
}
