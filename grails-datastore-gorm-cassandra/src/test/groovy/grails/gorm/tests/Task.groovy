package grails.gorm.tests

import grails.persistence.Entity

@Entity
class Task implements Serializable {
	UUID id
	Long version	
	String name

	static mapping = {
		name index:true
	}
	
}