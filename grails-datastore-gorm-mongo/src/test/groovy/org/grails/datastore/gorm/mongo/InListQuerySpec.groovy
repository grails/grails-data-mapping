package org.grails.datastore.gorm.mongo

import grails.gorm.tests.GormDatastoreSpec
import spock.lang.Issue
import grails.gorm.tests.Pet
import grails.gorm.tests.Person
import grails.gorm.tests.PetType

/**
 */
class InListQuerySpec extends GormDatastoreSpec{

    @Issue("GPMONGODB-160")
    void "Test that ne works for a single-ended association"() {
        given:"Some test data"
            createPets()
            session.clear()

        when:"Querying an association in a given list"
            def saurapod = PetType.findByName('Saurapod')

            def results = Pet.withCriteria {
                ne 'type', saurapod
                order "name"
            }

        then:"The correct results are returned"
        results.size() == 2
        results[0].name == "Flipper"
        results[1].name == "T-rex"
    }

    @Issue('GPMONGODB-161')
    void "Test that in queries work for single-ended associations"() {
        given:"Some test data"
            createPets()
            session.clear()
        
        when:"Querying an association in a given list"
            def list = PetType.withCriteria {
                or {
                    eq 'name', 'Tyrannosaur'
                    eq 'name', 'Saurapod'
                }
            }   
        
            assert list.size() == 2
            def results = Pet.withCriteria {
                inList 'type', list
                order "name"
            }

        then:"The correct results are returned"
            results.size() == 2
            results[0].name == "Dino"
            results[1].name == "T-rex"
    }

    void createPets() {
        def owner = new Person(firstName: "Fred", lastName: "Flintstone").save()
        assert owner != null
        def saurapod = new PetType(name: "Saurapod").save()
        def tyrannosaur = new PetType(name: "Tyrannosaur").save()
        def plesiosaur = new PetType(name: "Plesiosaur").save()
        assert new Pet(name: "Dino",owner: owner, type: saurapod).save()
        assert new Pet(name: "T-rex",owner: owner, type: tyrannosaur).save()
        assert new Pet(name: "Flipper",owner: owner, type: plesiosaur).save(flush:true)
    }
}
