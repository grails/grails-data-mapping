package grails.gorm.tests

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 04/11/2010
 * Time: 14:02
 * To change this template use File | Settings | File Templates.
 */
class OneToManySpec extends GormDatastoreSpec{

  void "test save and return unidirectional one to many"() {
	given:
	  Person p = new Person(firstName: "Fred", lastName: "Flinstone")
	  Country c = new Country(name:"Dinoville")
	  					.addToResidents(p)
						.save(flush:true)

	  session.clear()

	when:
	  c = Country.findByName("Dinoville")

	then:

	  c != null
	  c.residents != null
	  c.residents.size() == 1
	  
	when:
		c.addToResidents(firstName:"Barney", lastName:"Rubble")
		c.save(flush:true)
		session.clear()
		c = Country.findByName("Dinoville")
		
	then:
		c != null
		c.residents != null
		c.residents.size() == 2

  }
	
  void "test save and return bidirectional one to many"() {
    given:
      Person p = new Person(firstName: "Fred", lastName: "Flinstone")
      p.addToPets(new Pet(name: "Dino", type: new PetType(name: "Dinosaur")))
      p.save(flush:true)
	  
	  new Person(firstName: "Barney", lastName: "Rubble")
		  .addToPets(new Pet(name: "T Rex", type: new PetType(name: "Dinosaur")))
		  .addToPets(new Pet(name: "Stego", type: new PetType(name: "Dinosaur")))
		  .save(flush:true)

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
	  
	 when:
		 p.addToPets(new Pet(name: "Rex", type: new PetType(name: "Dinosaur")))
		 p.save(flush:true)
  	 	 session.clear() 
		 p = Person.findByFirstName("Fred")
		 
    then:	  
	  p != null
	  p.pets != null
	  p.pets.size() == 2
  }
  
  void "test update inverse side of bidirectional one to many collection"() {
	  given:
	  	Person p = new Person(firstName: "Fred", lastName: "Flinstone").save()
		new Pet(name: "Dino", type: new PetType(name: "Dinosaur"), owner:p).save()
		Person p2 = new Person(firstName: "Barney", lastName: "Rubble").save()
		new Pet(name: "T Rex", type: new PetType(name: "Dinosaur"), owner:p2).save()
		new Pet(name: "Stego", type: new PetType(name: "Dinosaur"), owner:p2).save(flush:true)
  
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
