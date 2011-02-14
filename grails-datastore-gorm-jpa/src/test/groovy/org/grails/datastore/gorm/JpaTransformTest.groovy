package org.grails.datastore.gorm

import grails.gorm.JpaEntity

import javax.persistence.Basic
import javax.persistence.CascadeType
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToMany
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.PostLoad
import javax.persistence.PostPersist
import javax.persistence.PostRemove
import javax.persistence.PostUpdate
import javax.persistence.PrePersist
import javax.persistence.PreRemove
import javax.persistence.PreUpdate
import javax.persistence.Table
import javax.persistence.Temporal
import javax.persistence.Transient
import javax.persistence.Version


class JpaTransformTest extends GroovyTestCase{

	void testSimpleAnnotatedEntity() {

		def ann = Simple.getAnnotation(Entity)
		
		assert ann != null
		
		def idField = Simple.getDeclaredField("id")
		
		assert idField != null
		
		def idAnn = idField.getAnnotation(Id.class)
		
		assert idAnn != null
		
		GeneratedValue genAnn = idField.getAnnotation(GeneratedValue.class)
		
		assert genAnn != null
		assert genAnn.strategy() == GenerationType.AUTO
		
		def ageField = Simple.getDeclaredField("age")
		
		assert ageField != null
		
		
		def ageAnn = ageField.getAnnotation(Transient)
		
		assert ageAnn != null
		
		def nameField = Simple.getDeclaredField("name")
		
		assert nameField != null
		
		def nameAnn = nameField.getAnnotation(Basic)
		
		assert nameAnn != null
		
		def dateCreatedField = Simple.getDeclaredField("dateCreated")
		
		assert dateCreatedField != null
		
		def dateCreatedAnn = dateCreatedField.getAnnotation(Temporal)
		
		assert dateCreatedAnn != null
		
				
		def addressField = Simple.class.getDeclaredField("address")
		
		assert addressField != null
		
		def addressAnn = addressField.getAnnotation(Embedded)
		
		assert addressAnn != null
		
		def personField = Simple.class.getDeclaredField("person")
		
		assert personField != null
		
		def personAnn = personField.getAnnotation(ManyToOne)
		
		assert personAnn != null
		
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
	
	void testJpaEventMethods() {
		def beforeInsert = Simple.class.getDeclaredMethod("beforeInsert", null)
		
		assert beforeInsert != null
		assert beforeInsert.getAnnotation(PrePersist) != null
		
		def afterInsert = Simple.class.getDeclaredMethod("afterInsert", null)
		
		assert afterInsert != null
		assert afterInsert.getAnnotation(PostPersist) != null
		
		def afterLoad = Simple.class.getDeclaredMethod("afterLoad", null)
		
		assert afterLoad != null
		assert afterLoad.getAnnotation(PostLoad) != null
		
		def beforeUpdate = Simple.class.getDeclaredMethod("beforeUpdate", null)
		
		assert beforeUpdate != null
		assert beforeUpdate.getAnnotation(PreUpdate) != null
		
		def afterUpdate = Simple.class.getDeclaredMethod("afterUpdate", null)
		
		assert afterUpdate != null
		assert afterUpdate.getAnnotation(PostUpdate) != null
		
		def beforeDelete = Simple.class.getDeclaredMethod("beforeDelete", null)
		
		assert beforeDelete != null
		assert beforeDelete.getAnnotation(PreRemove) != null
		
		def afterDelete = Simple.class.getDeclaredMethod("afterDelete", null)
		
		assert afterDelete != null
		assert afterDelete.getAnnotation(PostRemove) != null
	}
	
	void testCustomColumnMappingAndIdMapping() {
		Table tableAnn = Force.class.getAnnotation(Table)
		
		assert tableAnn != null
		assert tableAnn.name() == "the_force"
		def myIdField = Force.class.getDeclaredField("myId")
		
		assert myIdField.getAnnotation(Id) != null
		assert myIdField.getAnnotation(GeneratedValue) == null
		
		def versionField = Force.class.getDeclaredField("version")
		
		assert versionField != null
		assert versionField.getAnnotation(Version) == null
		
		shouldFail {
			Force.class.getDeclaredField("id")
		}
		
				
	}
}
@JpaEntity
class Simple {
	Long id
	Long version
	
	String name
	Integer age
	Address address
	Person person
	Date dateCreated
	
	static transients = ['age']
	static embedded = ["address"]
	
	def beforeInsert() {
		
	}
	
	def afterInsert() {
		
	}
	
	def afterLoad() {
		
	}
	
	def beforeUpdate() {
		
	}
	
	def afterUpdate() {
		
	}
	
	def beforeDelete() {
		
	}
	
	def afterDelete() {
		
	}
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
@JpaEntity
class Force {
	String myId
	Long version
	String name
	
	static constraints = {
		name size:5..15, nullable:true
	}
	
	static mapping = {
		table "the_force"
		id name:"myId", generator:"assigned"
		
		name column:"the_name"
		version false
	}
}
