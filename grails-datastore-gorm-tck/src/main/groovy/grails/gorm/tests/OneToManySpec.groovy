package grails.gorm.tests

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 04/11/2010
 * Time: 14:02
 * To change this template use File | Settings | File Templates.
 */
class OneToManySpec extends GormDatastoreSpec{

  void "test save and return one to many"() {
    given:
      Person p = new Person(firstName: "Fred", lastName: "Flinstone")
      p.addToPets(new Pet(name: "Dino", type: new PetType(name: "Dinosaur")))
      p.save(flush:true)

      session.clear()

    when:
      p = Person.findByFirstName("Fred")

    then:

      p != null
      p.pets != null
      p.pets.size() == 1
      def pet = p.pets.iterator().next()
      pet.name == 'Dino'
      pet.type != null
      pet.type.name == 'Dinosaur'
  }
}
