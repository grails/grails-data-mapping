package org.grails.datastore.gorm.mongo

import grails.gorm.tests.GormDatastoreSpec
import grails.persistence.Entity

class EmbeddedAssociationSpec extends GormDatastoreSpec {

    static {
        GormDatastoreSpec.TEST_CLASSES << Individual << Individual2 << Address
    }

    void "Test persistence of embedded entities"() {
        given:"A domain with an embedded association"
            def i = new Individual(name:"Bob", address: new Address(postCode:"30483"))

            i.save(flush:true)
            session.clear()

        when:"When domain is queried"
            i = Individual.findByName("Bob")

        then:"The embedded association is correctly loaded"
            i != null
            i.name == 'Bob'
            i.address != null
            i.address.postCode == '30483'

        when:"The embedded association is updated"
            i.address.postCode = "28749"
            i.save(flush:true)
            session.clear()
            i = Individual.get(i.id)


        then:"The embedded association is correctly updated"
            i != null
            i.name == 'Bob'
            i.address != null
            i.address.postCode == '28749'


        when:"An embedded association is queried"
            session.clear()
            i = Individual.createCriteria().get {
                address {
                    eq 'postCode', '28749'
                }
            }


        then:"The correct results are returned"
            i != null
            i.name == 'Bob'
            i.address != null
            i.address.postCode == '28749'


    }

    void "Test persistence of embedded entity collections"() {
        given:"An entity with an embedded entity collection"
            def i = new Individual2(name:"Bob", address: new Address(postCode:"30483"))
            i.otherAddresses = [new Address(postCode: "12345"), new Address(postCode: "23456")]
            i.save(flush:true)
            session.clear()

        when:"The entity is queried"
            i = Individual2.findByName("Bob")

        then:"The object was correctly persisted"
            i != null
            i.name == 'Bob'
            i.address != null
            i.address.postCode == '30483'
            i.otherAddresses != null
            i.otherAddresses.size() == 2
            i.otherAddresses[0].postCode == '12345'
            i.otherAddresses[1].postCode == '23456'


        when:"The embedded collection association is queried"
            def i2 = new Individual2(name:"Fred", address: new Address(postCode:"345334"))
            i2.otherAddresses = [new Address(postCode: "35432"), new Address(postCode: "34542")]
            i2.save(flush:true)

            session.clear()
            def results = Individual2.createCriteria().list {
                otherAddresses {
                    eq 'postCode', '23456'
                }
            }

        then:"The correct results are returned"
            results.size() == 1
            results[0].name == 'Bob'

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
