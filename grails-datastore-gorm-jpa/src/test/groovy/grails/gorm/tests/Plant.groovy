package grails.gorm.tests

import grails.gorm.JpaEntity 
import java.io.Serializable;


@JpaEntity
class Plant implements Serializable{
    boolean goesInPatch
    String name

	static mapping = {
		name index:true
		goesInPatch index:true
	}
}
