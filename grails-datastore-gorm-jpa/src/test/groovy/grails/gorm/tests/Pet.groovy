package grails.gorm.tests

import grails.gorm.JpaEntity 
import java.io.Serializable;
import java.util.Date;


@JpaEntity
class Pet implements Serializable {
	String name
	Date birthDate = new Date()
	PetType type
	Person owner

}
