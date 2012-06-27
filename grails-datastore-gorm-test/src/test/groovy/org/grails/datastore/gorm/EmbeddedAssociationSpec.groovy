package org.grails.datastore.gorm

import grails.gorm.tests.GormDatastoreSpec
import grails.persistence.Entity
import spock.lang.Shared

class EmbeddedAssociationSpec extends GormDatastoreSpec {

    static {
        GormDatastoreSpec.TEST_CLASSES << Individual << Address
    }

    @Shared Date now = new Date()

    void "Test persistence of embedded entities"() {
        given:
            def i = new Individual(name:"Bob", address: new Address(postCode:"30483"), bio: new Bio(birthday: new Birthday(now)))

            i.save(flush:true)
            session.clear()

        when:
            i = Individual.findByName("Bob")

        then:
            i != null
            i.name == 'Bob'
            i.address != null
            i.address.postCode == '30483'
            i.bio.birthday.date == now
    }
}

@Entity
class Individual {
    Long id
    String name
    Address address
    Bio bio

    static embedded = ['address', 'bio']

    static mapping = {
        name index:true
    }
}

@Entity
class Address {
    Long id
    String postCode
}

// Test embedded associations with custom types
class Bio {
    Birthday birthday
}