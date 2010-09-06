package grails.gorm.tests

import grails.persistence.Entity
import org.grails.datastore.gorm.proxy.GroovyProxyFactory

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Sep 6, 2010
 * Time: 11:39:48 AM
 * To change this template use File | Settings | File Templates.
 */
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
class Pet {
    Long id
	String name
	Date birthDate = new Date()
	PetType type
	Person owner

}

@Entity
class Person {
  Long id
  String firstName
  String lastName
  Set pets = [] as Set
  static hasMany = [pets:Pet]

  static mapping = {
    firstName index:true
    lastName index:true
  }
}

@Entity
class PetType  {
    Long id  
	String name
}
