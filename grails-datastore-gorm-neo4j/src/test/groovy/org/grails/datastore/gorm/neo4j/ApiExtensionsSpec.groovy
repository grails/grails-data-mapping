package org.grails.datastore.gorm.neo4j

import grails.gorm.tests.GormDatastoreSpec
import grails.gorm.tests.Person
import grails.gorm.tests.Pet
import grails.gorm.tests.PetType

/**
 * check the traverser extension
 */
class ApiExtensionsSpec extends GormDatastoreSpec {

    def "test cypher queries"() {
        setup:
        new Person(lastName:'person1').save()
        new Person(lastName:'person2').save()
        session.flush()
        session.clear()

        when:
        def result = Person.cypherStatic("MATCH (p:Person) RETURN p")

        then:
        result.iterator().size()==2

        when: "test with parameters"
        result = Person.cypherStatic("MATCH (p:Person) WHERE p.lastName={name} RETURN p", [name:'person1'])

        then:
        result.iterator().size()==1
    }

    def "test instance based cypher query"() {
        setup:
        def person = new Person(firstName: "Bob", lastName: "Builder")
        def petType = new PetType(name: "snake")
        def pet = new Pet(name: "Fred", type: petType, owner: person)
        person.addToPets(pet)
        person.save(flush: true)
        session.clear()

        when:
        def result = person.cypher("MATCH (p:Person)<-[:OWNER]->m WHERE p.__id__={this} return m")

        then:
        result.iterator().size() == 1
    }

}
