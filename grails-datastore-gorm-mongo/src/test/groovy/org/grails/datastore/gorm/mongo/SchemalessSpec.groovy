package org.grails.datastore.gorm.mongo

import grails.gorm.tests.GormDatastoreSpec;
import grails.gorm.tests.Plant;

class SchemalessSpec extends GormDatastoreSpec{

	def "Test attach additional data"() {
		given:
			def p = new Plant(name:"Pineapple")
			p.dbo.color = "Yellow"
			p.save(flush:true)
			session.clear()
			
		when:
			p = Plant.get(p.id)	
			
		then:
			p.name == 'Pineapple'
			p.dbo.color == 'Yellow'
			p['color'] == 'Yellow'
			
		when:
			p['hasLeaves'] = true
			p.save(flush:true)
			session.clear()
			p = Plant.get(p.id)
			
		then:
			p.name == 'Pineapple'
			p.dbo.color == 'Yellow'
			p['color'] == 'Yellow'
			p['hasLeaves'] == true
		
	}
}
