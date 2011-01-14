package org.grails.datastore.gorm

import grails.gorm.tests.GormDatastoreSpec;
import grails.persistence.Entity 

class EmbeddedAssociationSpec extends GormDatastoreSpec{
	static {
		GormDatastoreSpec.TEST_CLASSES << Individual << Address
	}
	
	
	void "Test persistence of embedded entities"() {
		given:
			def i = new Individual(name:"Bob", address: new Address(postCode:"30483"))
			
			i.save(flush:true)
			session.clear()
			
		when:
			i = Individual.findByName("Bob")
			
		then:
			i != null
			i.name == 'Bob'
			i.address != null
			i.address.postCode == '30483'
	}

}
@Entity
class Individual {
	Long id
	String name
	Address address
	static embedded = ['address']
	
	static mapping = {
		name index:true
	}
}
@Entity
class Address {
	Long id
	String postCode
}
