package org.grails.datastore.gorm.mongo

import grails.gorm.tests.GormDatastoreSpec
import grails.persistence.Entity
import spock.lang.IgnoreRest
import spock.lang.Issue

class EmbeddedAssociationSpec extends GormDatastoreSpec {

    @Override
    List getDomainClasses() {
        return [Individual, Individual2, Address, LongAddress]
    }

    @Issue('GPMONGODB-317')
    void "Test query with an embedded instance"() {

        given:"A domain with an embedded association"

            def address = new Address(postCode: "30483")
            def i = new Individual(name:"Bob", address: address)

            i.save(flush:true)
            session.clear()

        when:"We query with the embedded instance"
            i = Individual.findByAddress(address)

        then:"the result is correct"
            i != null
            i.name == "Bob"
            i.name == "Bob"


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

        when:"The embedded association is set to null"
            i.address = null
            i.save(flush:true)
            session.clear()
            i = Individual.get(i.id)

        then:"The embedded association is updated correctly"
            i.address == null

    }

    void "Test persistence of embedded entities with links to parent"() {
        given:"A domain with an embedded association"
            def i = new Individual(name:"Bob", address: new Address(postCode:"30483"))
            i.address.individual = i
            i.save(flush:true)
            session.clear()

        when:"When domain is queried"
            i = Individual.findByName("Bob")

        then:"The embedded association is correctly loaded"
            i != null
            i.name == 'Bob'
            i.address != null
            i.address.postCode == '30483'
            i.address.individual == i

        when:"The embedded association is updated"
            i.address = new Address(postCode: '28749')
            i.save(flush:true)
            session.clear()
            i = Individual.get(i.id)

        then:"The embedded association is correctly updated"
            i != null
            i.name == 'Bob'
            i.address != null
            i.address.postCode == '28749'
            i.address.individual == i
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

    void "Test persistence of embedded collections with links to parent"() {
        given:"A domain with an embedded association"
        def i = new Individual2(name:"Bob", address: new Address(postCode:"30483"))

        when:"A collection is added via addTo"
        [new Address(postCode: "12345"), new Address(postCode: "23456")].each { i.addToOtherAddresses(it) }

        then:"Back-links are populated"
        i.otherAddresses[0].individual2 == i
        i.otherAddresses[1].individual2 == i

        when:"Entity is saved and session is cleared"
        i.save(flush:true)
        session.clear()
        i = Individual2.findByName("Bob")

        then:"The object was correctly persisted"
        i != null
        i.name == 'Bob'
        i.otherAddresses != null
        i.otherAddresses.size() == 2
        i.otherAddresses[0].individual2 == i
        i.otherAddresses[1].individual2 == i

        when:"The embedded association is updated"
        i.otherAddresses = [new Address(postCode: '28749')]
        i.save(flush:true)
        session.clear()
        i = Individual2.get(i.id)

        then:"The embedded association is correctly updated"
        i != null
        i.name == 'Bob'
        i.otherAddresses != null
        i.otherAddresses.size() == 1
        i.otherAddresses[0].individual2 == i
    }

    void "Test persistence of embedded sub-class entities"() {
        given:"A domain with an embedded association"
            def i = new Individual(name:"Oliver", address: new LongAddress(postCode:"30483", firstLine: "1 High Street",
                city: "Timbuktu"))

            i.save(flush:true)
            session.clear()

        when:"When domain is queried"
            i = Individual.findByName("Oliver")

        then:"The embedded association is correctly loaded"
            i != null
            i.name == 'Oliver'
            i.address instanceof LongAddress
            i.address.postCode == '30483'
            i.address.firstLine == '1 High Street'
            i.address.city == 'Timbuktu'

        when:"The embedded association is updated"
            i.address.firstLine = "2 High Street"
            i.save(flush:true)
            session.clear()
            i = Individual.get(i.id)

        then:"The embedded association is correctly updated"
            i != null
            i.name == 'Oliver'
            i.address instanceof LongAddress
            i.address.firstLine == '2 High Street'

        when:"An embedded association is queried"
            session.clear()
            i = Individual.createCriteria().get {
                address {
                    eq 'city', 'Timbuktu'
                }
            }

        then:"The correct results are returned"
            i != null
            i.name == 'Oliver'
            i.address instanceof LongAddress
            i.address.city == 'Timbuktu'
    }

    void "Test persistence of embedded sub-class entity collection"() {
        given:"An entity with an embedded entity collection"
            def i = new Individual2(name:"Ed", address: new Address(postCode:"30483"))
            i.otherAddresses = [new LongAddress(postCode: "12345", city: 'Auckland', firstLine: '1 Long Road'),
                    new Address(postCode: "23456")]
            i.save(flush:true)
            session.clear()

        when:"The entity is queried"
            i = Individual2.findByName("Ed")

        then:"The object was correctly persisted"
            i != null
            i.name == 'Ed'
            i.address != null
            i.address.postCode == '30483'
            i.otherAddresses != null
            i.otherAddresses.size() == 2
            i.otherAddresses[0] instanceof LongAddress
            i.otherAddresses[0].postCode == '12345'
            i.otherAddresses[0].city == 'Auckland'
            !(i.otherAddresses[1] instanceof LongAddress)
            i.otherAddresses[1].postCode == '23456'

        when:"The embedded collection association is queried"
            def i2 = new Individual2(name:"Felix", address: new Address(postCode:"345334"))
            i2.otherAddresses = [new LongAddress(postCode: "35432", city: "London", firstLine: "1 Oxford Road"),
                    new Address(postCode: "34542")]
            i2.save(flush:true)

            session.clear()
            def results = Individual2.createCriteria().list {
                otherAddresses {
                    eq 'city', 'Auckland'
                }
            }

        then:"The correct results are returned"
            results.size() == 1
            results[0].name == 'Ed'
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
    Individual individual
    Individual2 individual2

    static belongsTo = [Individual, Individual2]

    static constraints = {
        individual nullable: true
        individual2 nullable: true
    }
}

@Entity
class LongAddress extends Address {
    String firstLine
    String city
}
