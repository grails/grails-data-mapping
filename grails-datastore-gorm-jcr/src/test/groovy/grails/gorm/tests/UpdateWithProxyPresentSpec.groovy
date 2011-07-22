package grails.gorm.tests

import grails.persistence.Entity

import org.grails.datastore.gorm.proxy.GroovyProxyFactory

class UpdateWithProxyPresentSpec extends GormDatastoreSpec {

    void "Test update entity with association proxies"() {
        given:
            session.mappingContext.setProxyFactory(new GroovyProxyFactory())
            def person = new Person(firstName:"Bob", lastName:"Builder")
            def petType = new PetType(name:"snake")
            def pet = new Pet(name:"Fred", type:petType, owner:person)
            person.pets << pet
            person.save(flush:true)
            session.clear()

        when:
            person = Person.get(person.id)
            person.firstName = "changed"
            person.save(flush:true)
            session.clear()
            person = Person.get(person.id)
            def personPet = person.pets.iterator().next()

        then:
            person.firstName == "changed"
            personPet.name == "Fred"
            personPet.id == pet.id
            personPet.owner.id == person.id
            personPet.type.name == 'snake'
            personPet.type.id == petType.id
    }
}

@Entity
class Pet implements Serializable {
    String id
    String name
    Date birthDate = new Date()
    PetType type
    Person owner
}

@Entity
class Person implements Serializable {
    String id
    String firstName
    String lastName
    Set pets = []
    static hasMany = [pets:Pet]

    static mapping = {
        firstName index:true
        lastName index:true
    }
}

@Entity
class PetType implements Serializable {
    String id
    String name
}
