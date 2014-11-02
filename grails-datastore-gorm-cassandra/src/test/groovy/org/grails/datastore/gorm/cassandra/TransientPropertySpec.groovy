package org.grails.datastore.gorm.cassandra

import grails.gorm.tests.GormDatastoreSpec
import grails.persistence.Entity

class TransientPropertySpec extends GormDatastoreSpec {

    void "Test that transient properties are not saved to Cassandra"() {
        when:"A doman with a transient property is saved"
            def c = new Cow(name: "Daisy", other:"foo").save(flush:true)
            def service = c.rodeoService
            session.clear()
            c = Cow.findByName("Daisy")

        then:"The transient instance is not persisted"
            c != null
            c.name == "Daisy"
            c.other == null
            c.rodeoService  != null
            c.rodeoService !=  service
    }

    @Override
    List getDomainClasses() {
        [Cow]
    }
}

class RodeoService {}

@Entity
class Cow {
    
    String name
    transient String other
    transient rodeoService = new RodeoService()
	
	static mapping = {
		id name:"name", generator:"assigned"
	}
}
