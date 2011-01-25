package grails.gorm.tests

import grails.gorm.JpaEntity 



@JpaEntity
class TestEntity {
	String name
	Integer age

	ChildEntity child
  
	static mapping = {
	  name index:true
	  age index:true
	  child index:true
	}
  
}
