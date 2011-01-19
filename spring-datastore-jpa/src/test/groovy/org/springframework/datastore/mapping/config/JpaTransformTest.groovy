package org.springframework.datastore.mapping.config

import javax.persistence.CascadeType;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id 
import javax.persistence.ManyToMany 
import javax.persistence.ManyToOne 
import javax.persistence.OneToMany 
import javax.persistence.OneToOne 
import javax.persistence.Transient 

import org.springframework.datastore.mapping.jpa.config.JpaEntity;

class JpaTransformTest extends GroovyTestCase{

	void testSimpleAnnotatedEntity() {

		def ann = Simple.getAnnotation(Entity)
		
		assert ann != null
		
		def idField = Simple.getDeclaredField("id")
		
		assert idField != null
		
		def idAnn = idField.getAnnotation(Id.class)
		
		assert idAnn != null
		
		def ageField = Simple.getDeclaredField("age")
		
		assert ageField != null
		
		def ageAnn = ageField.getAnnotation(Transient)
		
		assert ageAnn != null
		
				
		def addressField = Simple.class.getDeclaredField("address")
		
		assert addressField != null
		
		def addressAnn = addressField.getAnnotation(Embedded)
		
		assert addressAnn != null
		
	}
	
	void testBidirectionalOneToMany() {
		def petsField = Person.class.getDeclaredField("pets")
		
		assert petsField != null
		
		OneToMany petsAnn = petsField.getAnnotation(OneToMany)
		
		assert petsAnn != null
		
		assert petsAnn.targetEntity() == Pet
		assert petsAnn.mappedBy() == "owner"
		assert petsAnn.cascade()
		assert petsAnn.cascade()[0] == CascadeType.ALL
		
		
		def ownerField = Pet.class.getDeclaredField("owner")
		
		assert ownerField != null
		def ownerAnn = ownerField.getAnnotation(ManyToOne)
		assert ownerAnn != null
	}
	
	void testUnidirectionalOneToMany() {
		def addressesField = Person.class.getDeclaredField("addresses")
		
		assert addressesField != null
		OneToMany addressAnn = addressesField.getAnnotation(OneToMany)
		assert addressAnn != null
		
		
		assert addressAnn.targetEntity() == Address
		assert addressAnn.cascade()
		assert addressAnn.cascade()[0] == CascadeType.ALL

	}
	
	void testOneToOne() {
		def carField = Person.class.getDeclaredField("car")
		
		assert carField != null
		
		OneToOne carAnn = carField.getAnnotation(OneToOne)
		
		assert carAnn != null
		
		assert carAnn.optional() == false
		assert carAnn.cascade()
		assert carAnn.cascade()[0] == CascadeType.ALL 
		
		def ownerField = Car.class.getDeclaredField("owner")
		
		assert ownerField != null
		OneToOne ownerAnn = ownerField.getAnnotation(OneToOne)
		
		assert ownerAnn != null
		assert ownerAnn.mappedBy() == "car"
	}
	
	void testManyToMany() {
		def contractsField = Person.class.getDeclaredField("contracts")
		
		assert contractsField != null
		
		ManyToMany contractsAnn = contractsField.getAnnotation(ManyToMany)
		
		assert contractsAnn != null
		assert contractsAnn.targetEntity() == Contract
		assert contractsAnn.cascade()
		assert contractsAnn.cascade()[0] == CascadeType.ALL
		assert contractsAnn.mappedBy() == ""
		
		def peopleField = Contract.class.getDeclaredField("people")
		
		
		assert peopleField != null
		
		ManyToMany peopleAnn = peopleField.getAnnotation(ManyToMany)
		
		assert peopleAnn != null
		assert peopleAnn.mappedBy() == "contracts"
		assert peopleAnn.targetEntity() == Person.class

	}
}
@JpaEntity
class Simple {
	Long id
	Long version
	
	String name
	Integer age
	Address address
	static transients = ['age']
	static embedded = ["address"]
}
@JpaEntity
class Address {}

@JpaEntity
class Person {
	Long id
	
	Set pets
	Set addresses
	Set contracts
	Car car
	static hasOne = [car:Car]
	static hasMany = [pets:Pet,addresses:Address, contracts:Contract]
	
}
@JpaEntity
class Contract {
	
	Set people
	static hasMany = [people:Person]
	static belongsTo = Person
}
@JpaEntity
class Car {
	Person owner
	Integer doors = 4
	static belongsTo = [owner:Person]
}
@JpaEntity 
class Pet {
	Person owner
	static belongsTo = [ owner: Person ]
}
