package grails.gorm.tests

import grails.gorm.JpaEntity 
import javax.persistence.Embeddable 


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
@JpaEntity
class Individual {
	String name
	Address address
	static embedded = ['address']
	
	static mapping = {
		name index:true
	}
}
@JpaEntity
@Embeddable
class Address {
	String postCode
}