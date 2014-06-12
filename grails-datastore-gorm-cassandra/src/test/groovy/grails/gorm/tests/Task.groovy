package grails.gorm.tests

import grails.persistence.Entity

@Entity
class Task implements Serializable {
	UUID id
	Long version
	Set tasks
	Task task
	String name

	static mapping = {
		name index:true
	}

	static hasMany = [tasks:Task]
}