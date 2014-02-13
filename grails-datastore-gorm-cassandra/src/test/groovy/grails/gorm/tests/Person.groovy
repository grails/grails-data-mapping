package grails.gorm.tests

import grails.persistence.Entity

@Entity
class Person implements Serializable {
	UUID id
	Long version

	String firstName
	String lastName
	Integer age = 0
	Set pets = [] as Set
	static hasMany = [pets: Pet]

	String toString() {
		"Person{firstName='$firstName', id='$id', lastName='$lastName', pets=$pets}"
	}
}
