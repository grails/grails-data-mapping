package org.grails.datastore.gorm.mongo

import grails.gorm.tests.GormDatastoreSpec
import grails.persistence.Entity

class BasicCollectionTypeSpec extends GormDatastoreSpec{
	static {
		GormDatastoreSpec.TEST_CLASSES << MyCollections
	}

	def "Test persist basic collection types"() {
		given:
			def mc = new MyCollections(names:['Bob', 'Charlie'], pets:[chuck:"Dog", eddie:'Parrot'])
			mc.save(flush:true)
			
			session.clear()
			
		when:
			mc = MyCollections.get(mc.id)
			
		then:
			mc.names != null
			mc.names.size() > 0
	}
}

@Entity
class MyCollections {
	Long id
	List<String> names = []
	Map pets = [:]
}
