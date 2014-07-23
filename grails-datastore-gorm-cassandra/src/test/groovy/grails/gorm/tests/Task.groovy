package grails.gorm.tests

import grails.gorm.CassandraEntity

@CassandraEntity
class Task implements Serializable {
	UUID id
	Long version	
	String name

	static mapping = {
		name index:true
	}
	
}