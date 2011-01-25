package grails.gorm.tests

import grails.gorm.JpaEntity 
import java.io.Serializable;

@JpaEntity
class PetType implements Serializable {
	String name
}

