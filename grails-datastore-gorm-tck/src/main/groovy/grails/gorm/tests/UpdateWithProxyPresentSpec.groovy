package grails.gorm.tests

import grails.persistence.Entity

import org.grails.datastore.gorm.proxy.GroovyProxyFactory
import org.grails.datastore.gorm.query.transform.ApplyDetachedCriteriaTransform

/**
 * @author graemerocher
 */
class UpdateWithProxyPresentSpec extends GormDatastoreSpec {

    void "Test update entity with association proxies"() {
        given:
            session.mappingContext.setProxyFactory(new GroovyProxyFactory())
            def person = new Person(firstName:"Bob", lastName:"Builder")
            def petType = new PetType(name:"snake")
            def pet = new Pet(name:"Fred", type:petType, owner:person)
            person.addToPets(pet)
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

    @Override
    List getDomainClasses() {
        [Pet, Person, PetType]
    }
}

@Entity
class Pet implements Serializable {
    Long id
    Long version
    String name
    Date birthDate = new Date()
    PetType type = new PetType(name:"Unknown")
    Person owner
    Integer age
    Face face

    static mapping = {
        name index:true
    }

    static constraints = {
        owner nullable:true
        age nullable: true
        face nullable:true
    }
}

@Entity
@ApplyDetachedCriteriaTransform
class Person implements Serializable, Comparable<Person> {
    static simpsons = where {
         lastName == "Simpson"
    }

    Long id
    Long version
    String firstName
    String lastName
    Integer age = 0
    Set<Pet> pets = [] as Set
    static hasMany = [pets:Pet]
    Face face

//    static peopleWithOlderPets = where {
//        pets {
//            age > 9
//        }
//    }
//    static peopleWithOlderPets2 = where {
//        pets.age > 9
//    }

    static Person getByFirstNameAndLastNameAndAge(String firstName, String lastName, int age) {
        find( new Person(firstName: firstName, lastName: lastName, age: age) )
    }

    static mapping = {
        firstName index:true
        lastName index:true
        age index:true
    }

    static constraints = {
        face nullable:true
    }

    @Override
    int compareTo(Person t) {
        age <=> t.age
    }
}

@Entity
class PetType implements Serializable {
    Long id
    Long version
    String name

    static belongsTo = Pet
}
