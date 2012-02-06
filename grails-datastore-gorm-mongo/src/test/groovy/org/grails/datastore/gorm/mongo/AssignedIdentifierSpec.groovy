package org.grails.datastore.gorm.mongo

import grails.persistence.Entity
import grails.gorm.tests.GormDatastoreSpec
import spock.lang.Issue
import org.springframework.dao.DataIntegrityViolationException

/**
 * Tests for usage of assigned identifiers
 */
class AssignedIdentifierSpec extends GormDatastoreSpec{

    void "Test that entities can be saved, retrieved and updated with assigned ids"() {
        when:"An entity is saved with an assigned id"
            def r = new River(name:"Amazon", country: "Brazil")
            r.save flush:true
            session.clear()
            r = River.get("Amazon")

        then:"The entity can be retrieved"
            r != null
            r.name == "Amazon"
            r.country == "Brazil"

        when:"The entity is updated"
            r.country = "Argentina"
            r.save flush:true
            session.clear()
            r = River.get("Amazon")

        then:"The update is applied"
            r != null
            r.name == "Amazon"
            r.country == "Argentina"

        when:"The entity is deleted"
            r.delete(flush:true)

        then:"It is gone"
            River.count() == 0
            River.get("Amazon") == null
    }
    
    @Issue("GPMONGODB-152")
    void "Test that saving a second object with an assigned identifier produces an error"() {
        when:"An entity is saved with an assigned id"
            def r = new River(name:"Amazon", country: "Brazil")
            r.save flush:true
            session.clear()
            r = River.get("Amazon")

        then:"The entity can be retrieved"
            r != null
            r.name == "Amazon"
            r.country == "Brazil"
        
        when:"A second object with the same id is saved"
            r = new River(name:"Amazon", country: "Brazil")
            r.save flush:true
        
        then:"An error is produced"
            River.count() == 1
            thrown DataIntegrityViolationException

        when:"A new session is created"
            def totalRivers = 0
            River.withNewSession  {
                r = new River(name:"Nile", country: "Egype")
                r.save flush:true
                totalRivers = River.count()
            }

        then:"It is possible to save new instances"
            totalRivers == 2
    }

    @Issue("GPMONGODB-170")
    void "Test that assigned identifiers work with the constructor"() {
        when:"An entity is saved with an assigned id"
            def l = new Lake(id: "Lake Ontario", country: "Canada")
            l.save flush: true
            session.clear()
            l = Lake.get("Lake Ontario")
        
        
        then:"The object is correctly retrieved by assigned id"
            l != null
            l.id == "Lake Ontario"
            l.country == "Canada"
    }

    @Issue("GPMONGODB-170")
    void "Test that assigned identifiers work with property setting"() {
        when:"An entity is saved with an assigned id"
        def l = new Lake(country: "Canada")
        l.id = "Lake Ontario"
        l.save flush: true
        session.clear()
        l = Lake.get("Lake Ontario")


        then:"The object is correctly retrieved by assigned id"
        l != null
        l.id == "Lake Ontario"
        l.country == "Canada"
    }    
    @Override
    List getDomainClasses() {
        [River, Lake]
    }


}
@Entity
class River {
    String name
    String country
    static mapping = {
        id name:'name', generator:'assigned'
    }
}

@Entity
class Lake {
    String id
    String country
    static mapping = {
        id generator:'assigned'
    }
}
